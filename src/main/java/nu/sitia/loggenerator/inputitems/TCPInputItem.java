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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TCPInputItem extends AbstractInputItem {
    static final Logger logger = Logger.getLogger(TCPInputItem.class.getName());
    /** The address to bind to. If not specified, bind to all interfaces */
    protected String hostName;

    /** The port to listen on */
    protected String port;

    protected final Queue<String> result = new PriorityQueue<>();


    /**
     * Create a new TCPInputItem
     */
    public TCPInputItem(Configuration config) {
        super(config);
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("TCPInputItem. Read from a TCP socket\n" +
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
            ServerSocket serverSocket;
            if (hostName != null) {
                // Listen on a specified address
                serverSocket = new ServerSocket();
                SocketAddress socketAddress = new InetSocketAddress(hostName, portNumber);
                serverSocket.bind(socketAddress);

            } else {
                // Listen to broadcast in a separate thread
                serverSocket = new ServerSocket(portNumber);
            }
            new TCPInputHandler(serverSocket, result).start();
        } catch (SocketException e) {
            throw new RuntimeException("Socket exception", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException trying to bind to " + hostName + ":" + port, e);
        }
        if (hostName != null) {
            logger.info("Serving TCP server on " + hostName + ":" + port);
        } else {
            logger.info("Serving TCP server on port: " + port);
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
        List<String> toReturn = new ArrayList<>();
        int size = 0;
        synchronized (result) {
            while (result.peek() != null && (batchSize == 0 || size < batchSize)) {
                String event = result.element();
                toReturn.add(event);
                result.remove();
                size++;
            }
        }
        return toReturn;
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
        return "TCPInputItem" + System.lineSeparator() +
                hostName + System.lineSeparator() +
                port;
    }
}
