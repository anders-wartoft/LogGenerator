/*
 * Copyright 2022 sitia.nu https://github.com/anders-wartoft/LogGenerator
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nu.sitia.loggenerator.outputitems;


import nu.sitia.loggenerator.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TCPOutputItem extends AbstractOutputItem implements SendListener {
    static final Logger logger = Logger.getLogger(TCPOutputItem.class.getName());
    /** Default number of seconds to try to reconnect before giving up */
    static final long DEFAULT_RECONNECT_TIMEOUT_SECONDS = 300;
    /** Timeout for establishing a TCP connection */
    static final int CONNECT_TIMEOUT_MS = 5_000;
    /** First delay between reconnect attempts */
    static final long INITIAL_BACKOFF_MS = 500;
    /** Max delay between reconnect attempts */
    static final long MAX_BACKOFF_MS = 30_000;

    /** Set when the JVM is shutting down so reconnect loops don't block the shutdown */
    private static volatile boolean jvmShuttingDown = false;
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> jvmShuttingDown = true, "TCPOutputItem-shutdown"));
    }

    /** The address to use */
    protected String hostName;
    /** The port to connect to */
    protected String port;
    /** Seconds to try to reconnect. 0 = fail at once, negative = retry forever */
    protected long reconnectTimeoutSeconds = DEFAULT_RECONNECT_TIMEOUT_SECONDS;
    /** The socket to use */
    private volatile Socket socket;
    /** Set by the reader thread when the peer has closed (or reset) the connection */
    private volatile boolean peerClosed = false;
    /** Set when we have given up reconnecting. All further sends fail immediately. */
    private volatile boolean failed = false;
    /** Number of times a lost connection has been re-established */
    private volatile long reconnectCount = 0;

    /**
     * Constructor. Add the callback method from this class.
     * @param config The command line arguments
     */
    public TCPOutputItem(Configuration config) {
        super(config);
        super.addListener(this);
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println(getClass().getSimpleName() + ". Write to TCP socket\n" +
                    "Parameters:\n" +
                    "--hostname, -hn <hostname> The hostname to connect to\n" +
                    "--port, -p <port> The port to connect to\n" +
                    "--reconnect-timeout, -rt <seconds> How long to try to reconnect when the connection is lost " +
                    "before exiting with an error. 0 = exit immediately, -1 = retry forever. Default: " +
                    DEFAULT_RECONNECT_TIMEOUT_SECONDS + "\n");
            System.exit(1);
        }
        if (super.setParameter(key, value)) {
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--hostname") || key.equalsIgnoreCase("-hn"))) {
            this.hostName = value;
            logger.fine("hostname " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--port") || key.equalsIgnoreCase("-p"))) {
            this.port = value;
            logger.fine("port " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--reconnect-timeout") || key.equalsIgnoreCase("-rt"))) {
            try {
                this.reconnectTimeoutSeconds = Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Field reconnect-timeout is not a number: " + value);
            }
            logger.fine("reconnect-timeout " + value);
            return true;
        }
        return false;
    }

    @Override
    public boolean afterPropertiesSet() {
        if (null == this.hostName) {
            throw new RuntimeException("Missing --hostname");
        }
        if (null == this.port) {
            throw new RuntimeException("Missing --port");
        }

        final Pattern pattern = Pattern.compile("^\\d{1,5}$");
        final Matcher matcher = pattern.matcher(this.port);
        if (!matcher.matches()) {
            throw new RuntimeException("Field port contains illegal characters: " + this.port);
        }
        return true;
    }

    /**
     * Write to console
     * @param elements The element to write
     * @throws RuntimeException Not thrown here
     */
    @Override
    public void write(List<String> elements) throws RuntimeException {
        super.write(elements);
    }

    /**
     * Callback. What to do when the cache is full.
     * Writes to hostname:port. If the connection is lost, reconnect and resend the
     * batch. If no connection can be established within the reconnect timeout,
     * a RuntimeException is thrown.
     * @param toSend String to send
     */
    @Override
    public void send(List<String> toSend) {
        if (failed) {
            throw new RuntimeException("TCP connection to " + hostName + ":" + port + " is lost");
        }
        StringBuilder sb = new StringBuilder();
        toSend.forEach(s -> sb.append(s).append(System.lineSeparator()));
        byte[] data = sb.toString().getBytes(Charset.defaultCharset());
        logger.fine("Sending: " + toSend);

        long deadline = reconnectTimeoutSeconds < 0 ? Long.MAX_VALUE
                : System.currentTimeMillis() + reconnectTimeoutSeconds * 1000;
        long backoff = INITIAL_BACKOFF_MS;
        IOException lastException = null;
        boolean firstAttempt = true;
        while (true) {
            if (!firstAttempt || peerClosed || socket == null) {
                try {
                    if (firstAttempt) {
                        logger.warning("Connection to " + hostName + ":" + port
                                + (peerClosed ? " closed by peer" : " lost") + ". Reconnecting...");
                    }
                    connect();
                    reconnectCount++;
                    logger.info("Reconnected to " + hostName + ":" + port);
                } catch (IOException e) {
                    lastException = e;
                    logger.fine("Reconnect to " + hostName + ":" + port + " failed: " + e.getMessage());
                    firstAttempt = false;
                    long now = System.currentTimeMillis();
                    if (now >= deadline || jvmShuttingDown) {
                        break;
                    }
                    if (!sleep(Math.min(backoff, deadline - now))) {
                        break;
                    }
                    backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
                    continue;
                }
            }
            try {
                OutputStream output = socket.getOutputStream();
                output.write(data);
                output.flush();
                logger.finer("Sent message without exception");
                return;
            } catch (IOException e) {
                lastException = e;
                closeQuietly();
                if (firstAttempt) {
                    logger.warning("Failed to send to " + hostName + ":" + port + ": " + e.getMessage() + ". Reconnecting...");
                    firstAttempt = false;
                } else {
                    // Connected but the write failed again (e.g., peer accepts and closes). Back off.
                    long now = System.currentTimeMillis();
                    if (now >= deadline || jvmShuttingDown || !sleep(Math.min(backoff, deadline - now))) {
                        break;
                    }
                    backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
                }
                if (System.currentTimeMillis() >= deadline || jvmShuttingDown) {
                    break;
                }
            }
        }
        failed = true;
        closeQuietly();
        logger.severe("Giving up on connection to " + hostName + ":" + port);
        throw new RuntimeException("Socket send exception, unable to (re)connect to " + hostName + ":" + port, lastException);
    }

    /**
     * Number of times a lost connection has been re-established
     * @return The reconnect count
     */
    @Override
    public long getReconnectCount() {
        return reconnectCount;
    }

    /**
     * Sleep in short slices so that a JVM shutdown is noticed promptly.
     * @param ms Milliseconds to sleep
     * @return false if interrupted or the JVM is shutting down
     */
    private boolean sleep(long ms) {
        long end = System.currentTimeMillis() + ms;
        try {
            while (!jvmShuttingDown) {
                long left = end - System.currentTimeMillis();
                if (left <= 0) {
                    return true;
                }
                Thread.sleep(Math.min(left, 100));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /**
     * Create a connected socket. Overridden by subclasses, e.g., to add TLS.
     * @param host Host to connect to
     * @param portNumber Port to connect to
     * @return A connected socket
     * @throws IOException On connection errors
     */
    protected Socket createSocket(String host, int portNumber) throws IOException {
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, portNumber), CONNECT_TIMEOUT_MS);
        } catch (IOException e) {
            s.close();
            throw e;
        }
        return s;
    }

    /**
     * (Re)connect and start a thread that detects when the peer closes the connection.
     * @throws IOException On connection errors
     */
    private synchronized void connect() throws IOException {
        closeQuietly();
        Socket s = createSocket(hostName, Integer.parseInt(this.port));
        s.setKeepAlive(true);
        socket = s;
        peerClosed = false;
        Thread reader = new Thread(() -> watchForClose(s), "TCPOutputItem-reader-" + hostName + ":" + port);
        reader.setDaemon(true);
        reader.start();
    }

    /**
     * Read (and discard) anything the peer sends. When the stream ends or fails, the peer has
     * closed the connection, so flag it to make the next send reconnect.
     * @param s The socket to watch
     */
    private void watchForClose(Socket s) {
        byte[] buffer = new byte[1024];
        try {
            InputStream in = s.getInputStream();
            while (in.read(buffer) != -1) {
                // Discard
            }
        } catch (IOException e) {
            logger.log(Level.FINE, "Reader for " + hostName + ":" + port + " ended", e);
        }
        if (s == socket) {
            peerClosed = true;
        }
    }

    private synchronized void closeQuietly() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.log(Level.FINE, "Socket close exception", e);
            }
            socket = null;
        }
    }

    @Override
    public void setup() throws RuntimeException {
        super.setup();
        long deadline = reconnectTimeoutSeconds < 0 ? Long.MAX_VALUE
                : System.currentTimeMillis() + reconnectTimeoutSeconds * 1000;
        long backoff = INITIAL_BACKOFF_MS;
        boolean firstAttempt = true;
        while (true) {
            try {
                connect();
                if (!firstAttempt) {
                    logger.info("Connected to " + hostName + ":" + port);
                }
                return;
            } catch (IOException e) {
                if (firstAttempt) {
                    logger.warning("Cannot connect to " + hostName + ":" + port + ": " + e.getMessage()
                            + ". Waiting for the receiver...");
                    firstAttempt = false;
                } else {
                    logger.fine("Connect to " + hostName + ":" + port + " failed: " + e.getMessage());
                }
                long now = System.currentTimeMillis();
                if (now >= deadline || jvmShuttingDown || !sleep(Math.min(backoff, deadline - now))) {
                    failed = true;
                    throw new RuntimeException("Exception trying to connect to: " + hostName + ":" + port, e);
                }
                backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
            }
        }
    }

    @Override
    public void teardown() {
        try {
            super.teardown();
        } finally {
            closeQuietly();
        }
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + System.lineSeparator() +
                hostName + System.lineSeparator() +
                port;
    }
}
