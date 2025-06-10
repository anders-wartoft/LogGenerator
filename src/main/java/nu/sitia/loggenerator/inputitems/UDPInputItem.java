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

package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.Configuration;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UDPInputItem extends AbstractInputItem {
    private static final int MAX_DATAGRAM_SIZE = 65507;
    static final Logger logger = Logger.getLogger(UDPInputItem.class.getName());
    /** The name to bind to */
    private String hostName;
    /** The port to listen on */
    private String port;
    /** The socket to use */
    private DatagramSocket socket;

    /**
     * Create a new UDPInputItem
     */
    public UDPInputItem(Configuration config) {
        super(config);
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("UDPInputItem. Read from a UDP socket\n" +
                    "Parameters:\n" +
                    "--hostname <hostname> (-hn <hostname>)\n" +
                    "  The hostname to bind to. If not specified, bind to all interfaces\n" +
                    "--port <port> (-p <port>)\n" +
                    "  The port to listen on\n");
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
        return false;
    }

    @Override
    public boolean afterPropertiesSet() {
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
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
        try {
            int portNumber = Integer.parseInt(this.port);
            if (hostName != null) {
                // Listen on a specified address
                socket = new DatagramSocket(new InetSocketAddress(hostName, portNumber));
            } else {
                // Listen to broadcast
                socket = new DatagramSocket(new InetSocketAddress(portNumber));
            }
        } catch (SocketException e) {
            throw new RuntimeException("Socket exception for " + hostName + ": " + port, e);
        }
        if (hostName != null) {
            logger.info("Serving UDP server on " + hostName + ":" + port);
        } else {
            logger.info("Serving UDP server on port " + port);
        }
    }

    /**
     * Are there more messages to read?
     * @return True iff there are more messages
     */
    public boolean hasNext() {
        return true;
    }

    /**
     * Read input one line at a time and return
     * {batchSize} elements.
     * @return The read input
     */
    public List<String> next() {
        List<String> result = new ArrayList<>();
        int lines = this.batchSize;
        while (lines-- > 0) {
            byte [] buffer = new byte[MAX_DATAGRAM_SIZE  + 1];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(response);
                logger.log(Level.FINE, "Received connection from " + response.getAddress());
                String received = new String(buffer, 0, response.getLength());
                logger.log(Level.FINE, received);
                result.add(received);
            } catch (IOException e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Socket receive interrupted");
                }
            }
        }
        return result;
    }

    /**
     * Let the item teardown after reading
     */
    public void teardown() {
        // Unreachable code
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "UDPInputItem" + System.lineSeparator() +
                hostName + System.lineSeparator() +
                port;
    }
}
