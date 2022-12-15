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
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

public class UDPOutputItem extends AbstractOutputItem implements SendListener {
    static final Logger logger = Logger.getLogger(UDPOutputItem.class.getName());
    /** The InetAddress to connect to */
    private final InetAddress address;
    /** Port number to connect to */
    private final int port;
    /** The socket to use */
    private final DatagramSocket socket;

    /** Used in toString() */
    private final String hostName;

    /**
     * Constructor. Add the callback method from this class.
     * @param config The command line arguments
     */
    public UDPOutputItem(Configuration config) {
        super(config);
        String hostName = config.getValue("-oh");
        if (null == hostName) {
            throw new RuntimeException(config.getNotFoundInformation("-oh"));
        }
        String portString = config.getValue("-op");
        if (null == portString) {
            throw new RuntimeException(config.getNotFoundInformation("-op"));
        }
        port = Integer.parseInt(portString);

        super.addListener(this);
        try {
            address = InetAddress.getByName(hostName);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unknown host: " + hostName, e);
        }
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException("Socket exception", e);
        }
        this.hostName = hostName;
        addTransactionMessages = true;
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
        for (String data : toSend) {
            byte[] buffer = data.getBytes(StandardCharsets.UTF_8);
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, port);
            try {
                logger.fine("Sending: " + toSend);
                socket.send(request);
                logger.finer("Sent message without exception");
            } catch (IOException e) {
                throw new RuntimeException("Socket send exception", e);
            }
        }
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "UDPOutputItem" + System.lineSeparator() +
                hostName + System.lineSeparator() +
                port;
    }
}
