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

public class TCPInputItem extends AbstractInputItem {
    static final Logger logger = Logger.getLogger(TCPInputItem.class.getName());
    /** The address to bind to. If not specified, bind to all interfaces */
    protected final String hostName;

    /** The port to listen on */
    protected final int port;

    protected final Queue<String> result = new PriorityQueue<>();


    /**
     * Create a new TCPInputItem
     * @param config The command line arguments
     */
    public TCPInputItem(Configuration config) {
        super(config);
        hostName = config.getValue("-ih");
        String portString = config.getValue("-ip");
        if (null == portString) {
            throw new RuntimeException(config.getValue("-ip"));
        }
        port = Integer.parseInt(portString);
    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
        try {
            ServerSocket serverSocket;
            if (hostName != null) {
                // Listen on a specified address
                serverSocket = new ServerSocket();
                SocketAddress socketAddress = new InetSocketAddress(hostName, port);
                serverSocket.bind(socketAddress);

            } else {
                // Listen to broadcast in a separate thread
                serverSocket = new ServerSocket(port);
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
