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
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nu.sitia.loggenerator.outputitems;


import nu.sitia.loggenerator.Configuration;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.List;
import java.util.logging.Logger;

public class TCPOutputItem extends AbstractOutputItem implements SendListener {
    static final Logger logger = Logger.getLogger(TCPOutputItem.class.getName());
    /** The address to use */
    protected final String hostName;
    /** The port to connect to */
    protected final int port;
    /** The socket to use */
    private Socket socket;

    /**
     * Constructor. Add the callback method from this class.
     * @param config The command line arguments
     */
    public TCPOutputItem(Configuration config) {
        super(config);
        super.addListener(this);
        hostName = config.getValue("-oh");
        if (null == hostName) {
            throw new RuntimeException(config.getNotFoundInformation("-oh"));
        }
        String portString = config.getValue("-op");
        if (null == portString) {
            throw new RuntimeException(config.getNotFoundInformation("-op"));
        }
        port = Integer.parseInt(portString);
        addTransactionMessages = config.isStatistics();
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
     * Writes to hostname:port
     * @param toSend String to send
     */
    @Override
    public void send(List<String> toSend) {
        final boolean autoFlush = true;
        try {
            OutputStream output = socket.getOutputStream();
            logger.fine("Sending: " + toSend);
            PrintWriter writer = new PrintWriter(output, autoFlush);
            toSend.forEach(writer::println);
            logger.finer("Sent message without exception");
        } catch (IOException e) {
            throw new RuntimeException("Socket send exception", e);
        }
    }

    @Override
    public void setup() throws RuntimeException {
        super.setup();
        try {
            socket = new Socket(hostName, port);
        } catch (IOException e) {
            throw new RuntimeException("Socket exception", e);
        }
    }

    @Override
    public void teardown() {
        super.teardown();
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Socket close exception", e);
        }
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "TCPOutputItem" + System.lineSeparator() +
                hostName + System.lineSeparator() +
                port;
    }
}
