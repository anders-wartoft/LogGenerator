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
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

public class RelpOutputItem extends AbstractOutputItem implements SendListener {
    static final Logger logger = Logger.getLogger(RelpOutputItem.class.getName());
    
    /** The hostname to connect to */
    private String hostname;
    
    /** The port to connect to */
    private int port;
    
    /** Socket connection */
    private Socket socket;
    
    /** Output stream */
    private OutputStream outputStream;
    
    /** Input stream for reading responses */
    private BufferedReader inputReader;
    
    /** Transaction number for RELP protocol */
    private int transactionNr = 1;
    
    /**
     * Constructor. Add the callback method from this class.
     * @param config The command line arguments
     */
    public RelpOutputItem(Configuration config) {
        super(config);
        super.addListener(this);
    }
    
    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("RelpOutputItem. Write to rsyslog via RELP protocol\n" +
                    "Parameters:\n" +
                    "--hostname, -hn <hostname> The hostname to connect to\n" +
                    "--port, -p <port> The port to connect to\n");
            System.exit(1);
        }
        if (super.setParameter(key, value)) {
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--hostname") || key.equalsIgnoreCase("-hn"))) {
            this.hostname = value;
            logger.fine("hostname " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--port") || key.equalsIgnoreCase("-p"))) {
            try {
                this.port = Integer.parseInt(value);
                logger.fine("port " + value);
                return true;
            } catch (NumberFormatException e) {
                throw new RuntimeException("Port must be a valid integer: " + value);
            }
        }
        return false;
    }
    
    @Override
    public boolean afterPropertiesSet() {
        if (null == this.hostname) {
            throw new RuntimeException("Missing --hostname");
        }
        if (this.port <= 0 || this.port > 65535) {
            throw new RuntimeException("Port must be between 1 and 65535");
        }
        return true;
    }
    
    /**
     * Write to RELP server
     * @param elements The elements to write
     * @throws RuntimeException if connection fails
     */
    @Override
    public void write(List<String> elements) throws RuntimeException {
        super.write(elements);
    }
    
    /**
     * Callback. What to do when the cache is full.
     * Sends to hostname:port via RELP protocol
     * @param toSend Strings to send
     */
    @Override
    public void send(List<String> toSend) {
        logger.fine("Sending " + toSend.size() + " messages to " + hostname + ":" + port);
        if (toSend.size() > 0) {
            for (String message : toSend) {
                sendRelpMessage(message);
            }
        }
    }
    
    /**
     * Send a single message via RELP protocol
     * @param message The message to send
     */
    private void sendRelpMessage(String message) {
        try {
            String relpFrame = createRelpFrame("syslog", message);
            logger.finest("Sending RELP frame: " + relpFrame);
            outputStream.write(relpFrame.getBytes());
            outputStream.flush();
            
            // Read response
            String response = inputReader.readLine();
            if (response == null || !response.contains("rsp")) {
                throw new RuntimeException("No response from RELP server or invalid response");
            }
            logger.finer("Received RELP response: " + response);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send RELP message: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a RELP frame
     * Format: txnr command len data\n
     * @param command The RELP command (e.g., "syslog")
     * @param data The data to send
     * @return The formatted RELP frame
     */
    private String createRelpFrame(String command, String data) {
        int len = data.getBytes().length;
        String frame = transactionNr + " " + command + " " + len + " " + data + "\n";
        transactionNr++;
        return frame;
    }
    
    @Override
    public void setup() throws RuntimeException {
        super.setup();
        try {
            socket = new Socket(hostname, port);
            outputStream = socket.getOutputStream();
            inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Send RELP OPEN command
            String openFrame = createRelpFrame("open", "RELP_VERSION=0\nRELP_SOFTWARE=LogGenerator\nRELP_COMMANDS=syslog");
            outputStream.write(openFrame.getBytes());
            outputStream.flush();
            
            // Read open response
            String response = inputReader.readLine();
            if (response == null || !response.contains("rsp")) {
                throw new RuntimeException("Failed to open RELP connection");
            }
            logger.info("Connected to RELP server " + hostname + ":" + port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to RELP server: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void teardown() {
        super.teardown();
        try {
            if (outputStream != null) {
                String closeFrame = createRelpFrame("close", "");
                outputStream.write(closeFrame.getBytes());
                outputStream.flush();
            }
            if (inputReader != null) {
                inputReader.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            logger.info("Disconnected from RELP server");
        } catch (IOException e) {
            logger.warning("Error closing RELP connection: " + e.getMessage());
        }
    }
    
    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "RelpOutputItem" + System.lineSeparator() +
                hostname + ":" + port;
    }
}