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

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SSLTCPInputItem extends TCPInputItem {
    static final Logger logger = Logger.getLogger(SSLTCPInputItem.class.getName());

    /** The socket to use */
    private SSLServerSocket serverSocket;

    /**
     * Create a new SSLTCPInputItem
     */
    public SSLTCPInputItem(Configuration config) {
        super(config);
    }


    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
        try {
            int portNumber = Integer.parseInt(this.port);
            ServerSocketFactory sslSf = SSLServerSocketFactory.getDefault();

            if (hostName != null) {
                // Listen on a specified address
                serverSocket = (SSLServerSocket) sslSf.createServerSocket();
                SocketAddress socketAddress = new InetSocketAddress(hostName, portNumber);
                serverSocket.bind(socketAddress);

            } else {
                // Listen to broadcast in a separate thread
                serverSocket = (SSLServerSocket) sslSf.createServerSocket(portNumber);
            }
            new TCPInputHandler(serverSocket, result).start();

        } catch (SocketException e) {
            throw new RuntimeException("Socket exception", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException trying to bind to " + hostName + ":" + port, e);
        }
        if (hostName != null) {
            logger.info("Serving TCP SSL server on " + hostName + ":" + port);
        } else {
            logger.info("Serving TCP SSL server on port: " + port);
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
                size ++;
            }
        }
        return toReturn;
    }


    /**
     * Let the item teardown after reading
     */
    public void teardown() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "SSLTCPInputItem" + System.lineSeparator() +
                hostName + System.lineSeparator() + port;
    }

}

