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
import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

public class SSLTCPOutputItem extends TCPOutputItem {
    static final Logger logger = Logger.getLogger(SSLTCPOutputItem.class.getName());

    /** The socket to use */
    private Socket sslSocket;

    /**
     * Constructor. Add the callback method from this class.
     * @param config The command line arguments
     */
    public SSLTCPOutputItem(Configuration config) {
        super(config);
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

    @Override
    public void setup() throws RuntimeException {
        // do NOT call super.setup() here. If so, another socket will be created to
        // hostName:port, but not with SSL. That will ruin this connection...
        try {
            int portNumber = Integer.parseInt(this.port);

            SSLSocketFactory sslSf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslSocket = sslSf
                    .createSocket(hostName, portNumber);
        } catch (Exception e) {
            throw new RuntimeException("Exception trying to create client ssl socket to: " + hostName + ":" + port, e);
        }
    }


    /**
     * Callback. What to do when the cache is full.
     * Writes to hostname:port
     * @param toSend String to send
     */
    @Override
    public void send(List<String> toSend) {
        try {
            OutputStream output = sslSocket.getOutputStream();
            logger.fine("Sending: " + toSend);
            PrintWriter writer = new PrintWriter(output, true);
            toSend.forEach(writer::println);
            logger.finer("Sent message without exception");
        } catch (IOException e) {
            throw new RuntimeException("Socket send exception", e);
        }
    }

    @Override
    public void teardown() {
        super.teardown();
        try {
            sslSocket.close();
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
        return "SSLTCPOutputItem" + System.lineSeparator() +
                hostName + System.lineSeparator() +
                port;
    }

}
