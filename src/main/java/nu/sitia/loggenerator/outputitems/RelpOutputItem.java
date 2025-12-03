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
import java.net.SocketTimeoutException;
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
    
    /** Flag to track if connection is open */
    private volatile boolean connected = false;

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
     * Reconnect to the RELP server after disconnection
     */
    private void reconnect() throws IOException {
        logger.info("Reconnecting to RELP server...");
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.warning("Error closing old socket: " + e.getMessage());
        }
        
        socket = new Socket(hostname, port);
        socket.setSoTimeout(5000);
        outputStream = socket.getOutputStream();
        inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        // Send RELP OPEN command with proper offers
        String openData = "relp_version=0\nrelp_software=LogGenerator\ncommands=syslog";
        String openFrame = "1 open " + openData.length() + " " + openData + "\n";
        logger.fine("Sending OPEN frame (reconnect): " + openFrame);
        outputStream.write(openFrame.getBytes());
        outputStream.flush();
        
        String response = readRelpResponse();
        logger.fine("Received OPEN response (reconnect): " + response);
        
        if (response == null || !response.contains("rsp")) {
            throw new RuntimeException("Failed to reconnect to RELP server. Response: " + response);
        }
        
        transactionNr = 2;
        connected = true;
        logger.info("Reconnected to RELP server");
    }

    /**
     * Send a single message via RELP protocol
     * @param message The message to send
     */
    private void sendRelpMessage(String message) {
        sendRelpMessage(message, 0);
    }
    
    /**
     * Read a RELP response from the server
     * @return The complete RELP response
     * @throws IOException if reading fails
     */
    private String readRelpResponse() throws IOException {
        // Read the response line: txnr command len [data]
        String line = inputReader.readLine();
        if (line == null) {
            return null;
        }
        
        logger.finest("Read RELP response line: " + line);
        
        // Parse the header: txnr command len [data]
        String[] parts = line.split(" ", 4);
        if (parts.length < 3) {
            return line; // Return as-is if not proper RELP format
        }
        
        try {
            int dataLen = Integer.parseInt(parts[2]);
            
            if (dataLen == 0) {
                // No data
                return line;
            }
            
            // Check if data is already on the same line
            if (parts.length == 4 && parts[3].length() >= dataLen) {
                return line;
            }
            
            // Data is on subsequent lines or partially on this line
            // Read exactly dataLen bytes
            char[] dataBuffer = new char[dataLen];
            int totalRead = 0;
            
            // If there's partial data on the header line, copy it first
            if (parts.length == 4 && parts[3].length() > 0) {
                int partialLen = Math.min(parts[3].length(), dataLen);
                parts[3].getChars(0, partialLen, dataBuffer, 0);
                totalRead = partialLen;
            }
            
            // Read the remaining data
            while (totalRead < dataLen) {
                int read = inputReader.read(dataBuffer, totalRead, dataLen - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }
            
            // Reconstruct the full response
            String data = new String(dataBuffer, 0, totalRead);
            String result = parts[0] + " " + parts[1] + " " + parts[2] + " " + data;
            logger.finest("Reconstructed RELP response: " + result);
            return result;
            
        } catch (NumberFormatException e) {
            return line; // Return as-is if len is not a number
        }
    }

    /**
     * Send a single message via RELP protocol with retry logic
     * @param message The message to send
     * @param retryCount Current retry count to prevent infinite recursion
     */
    private void sendRelpMessage(String message, int retryCount) {
        if (retryCount > 3) {
            throw new RuntimeException("Failed to send message after " + retryCount + " retries. Check rsyslog RELP configuration.");
        }
        
        try {
            if (!connected) {
                logger.fine("Not connected, attempting reconnect (retry " + retryCount + ")");
                try {
                    Thread.sleep(200);
                    reconnect();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to reconnect: " + e.getMessage(), e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during reconnect: " + e.getMessage(), e);
                }
            }
            
            String relpFrame = createRelpFrame("syslog", message);
            logger.finest("Sending RELP frame: " + relpFrame);
            outputStream.write(relpFrame.getBytes());
            outputStream.flush();
            
            // Read response with timeout
            String response = readRelpResponse();
            if (response == null) {
                connected = false;
                logger.fine("Connection closed by server, retrying (attempt " + (retryCount + 1) + ")");
                sendRelpMessage(message, retryCount + 1);
                return;
            }
            
            logger.finer("Received RELP response: " + response);
            
            // Check if server is closing
            if (response.contains("serverclose")) {
                connected = false;
                logger.fine("Server sent serverclose, retrying (attempt " + (retryCount + 1) + ")");
                sendRelpMessage(message, retryCount + 1);
                return;
            }
            
            // Parse RELP response: txnr command len data
            // Example: "2 rsp 2 OK" or "2 rsp 0 "
            String[] parts = response.split(" ", 4);
            if (parts.length < 2) {
                throw new RuntimeException("Invalid response from RELP server: " + response);
            }
            
            String command = parts[1];
            if (!"rsp".equalsIgnoreCase(command)) {
                throw new RuntimeException("Invalid response from RELP server (expected 'rsp', got '" + command + "'): " + response);
            }
            
            // Success - message sent
            logger.finest("Message sent successfully");
            
        } catch (IOException e) {
            connected = false;
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
            socket.setSoTimeout(5000); // 5 second timeout for reads
            outputStream = socket.getOutputStream();
            inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Send RELP OPEN command with proper offers
            // Format: txnr command len data\n
            // Data format: relp_version=0\nrelp_software=LogGenerator\ncommands=syslog
            String openData = "relp_version=0\nrelp_software=LogGenerator\ncommands=syslog";
            String openFrame = "1 open " + openData.length() + " " + openData + "\n";
            logger.fine("Sending OPEN frame: " + openFrame);
            outputStream.write(openFrame.getBytes());
            outputStream.flush();
            
            // Read open response with timeout
            String response = readRelpResponse();
            logger.fine("Received OPEN response: " + response);
            
            if (response == null) {
                throw new RuntimeException("No response from RELP server");
            }
            
            // Check for valid response
            if (!response.contains("rsp")) {
                throw new RuntimeException("Failed to open RELP connection. Response: " + response);
            }
            
            connected = true;
            transactionNr = 2; // Next transaction number after OPEN
            logger.info("Connected to RELP server " + hostname + ":" + port);
        } catch (SocketTimeoutException e) {
            connected = false;
            throw new RuntimeException("Timeout connecting to RELP server: " + e.getMessage(), e);
        } catch (IOException e) {
            connected = false;
            throw new RuntimeException("Failed to connect to RELP server: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void teardown() {
        super.teardown();
        connected = false;
        try {
            if (outputStream != null) {
                String closeFrame = createRelpFrame("close", "");
                outputStream.write(closeFrame.getBytes());
                outputStream.flush();
                inputReader.readLine(); // Read response
            }
        } catch (Exception e) {
            logger.warning("Error sending CLOSE: " + e.getMessage());
        }
        
        try {
            if (inputReader != null) {
                inputReader.close();
            }
        } catch (IOException e) {
            logger.warning("Error closing reader: " + e.getMessage());
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.warning("Error closing socket: " + e.getMessage());
        }
        
        logger.info("Disconnected from RELP server");
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