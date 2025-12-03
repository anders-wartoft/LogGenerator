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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class RelpInputItem extends AbstractInputItem {
    static final Logger logger = Logger.getLogger(RelpInputItem.class.getName());
    
    /** The port to listen on */
    private int port;
    
    /** Server socket */
    private ServerSocket serverSocket;
    
    /** Thread pool for handling clients */
    private ExecutorService executorService;
    
    /** Flag to stop the server */
    private volatile boolean running = false;
    
    /** Thread for accepting connections */
    private Thread acceptThread;
    
    /** Queue of received messages */
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    
    /**
     * Constructor
     * @param config The command line arguments
     */
    public RelpInputItem(Configuration config) {
        super(config);
    }
    
    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("RelpInputItem. Receive from rsyslog via RELP protocol\n" +
                    "Parameters:\n" +
                    "--port, -p <port> The port to listen on\n");
            System.exit(1);
        }
        if (super.setParameter(key, value)) {
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
        if (this.port <= 0 || this.port > 65535) {
            throw new RuntimeException("Port must be between 1 and 65535");
        }
        return true;
    }
    
    @Override
    public void setup() throws RuntimeException {
        try {
            serverSocket = new ServerSocket(port);
            executorService = Executors.newFixedThreadPool(10);
            running = true;
            
            acceptThread = new Thread(this::acceptConnections);
            acceptThread.setDaemon(true);
            acceptThread.start();
            
            logger.info("RELP server listening on port " + port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start RELP server: " + e.getMessage(), e);
        }
    }
    
    /**
     * Accept incoming RELP connections
     */
    private void acceptConnections() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            if (running) {
                logger.warning("Error accepting connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle a single RELP client connection
     * @param clientSocket The client socket
     */
    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream output = clientSocket.getOutputStream();
            
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleRelpFrame(line, output);
            }
            
            reader.close();
            clientSocket.close();
        } catch (IOException e) {
            logger.warning("Error handling client: " + e.getMessage());
        }
    }
    
    /**
     * Handle a single RELP frame
     * Format: txnr command len data
     * @param frame The RELP frame
     * @param output The output stream for sending responses
     */
    private void handleRelpFrame(String frame, OutputStream output) throws IOException {
        String[] parts = frame.split(" ", 4);
        
        if (parts.length < 3) {
            logger.warning("Invalid RELP frame: " + frame);
            return;
        }
        
        String txnr = parts[0];
        String command = parts[1];
        int len = Integer.parseInt(parts[2]);
        String data = parts.length > 3 ? parts[3] : "";
        
        logger.finest("RELP frame - txnr: " + txnr + ", command: " + command + ", len: " + len + ", data: " + data);
        
        if ("open".equalsIgnoreCase(command)) {
            handleOpen(txnr, output);
        } else if ("syslog".equalsIgnoreCase(command)) {
            handleSyslog(txnr, data, output);
        } else if ("close".equalsIgnoreCase(command)) {
            handleClose(txnr, output);
        } else {
            logger.warning("Unknown RELP command: " + command);
        }
    }
    
    /**
     * Handle RELP OPEN command
     * @param txnr Transaction number
     * @param output Output stream
     */
    private void handleOpen(String txnr, OutputStream output) throws IOException {
        String response = txnr + " rsp 6 OK\n";
        output.write(response.getBytes());
        output.flush();
        logger.finer("Sent OPEN response");
    }
    
    /**
     * Handle RELP SYSLOG command
     * @param txnr Transaction number
     * @param data The syslog message
     * @param output Output stream
     */
    private void handleSyslog(String txnr, String data, OutputStream output) throws IOException {
        try {
            messageQueue.put(data);
            logger.finer("Received syslog message: " + data);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Interrupted while queueing message");
        }
        
        String response = txnr + " rsp 6 OK\n";
        output.write(response.getBytes());
        output.flush();
        logger.finer("Sent SYSLOG response");
    }
    
    /**
     * Handle RELP CLOSE command
     * @param txnr Transaction number
     * @param output Output stream
     */
    private void handleClose(String txnr, OutputStream output) throws IOException {
        String response = txnr + " rsp 6 OK\n";
        output.write(response.getBytes());
        output.flush();
        logger.finer("Sent CLOSE response");
    }

    @Override
    public boolean hasNext() throws RuntimeException {
        return true; // Always ready to read
    }

    @Override
    public List<String> next() throws RuntimeException {
        long maxWaitMillis = 1000;
        List<String> messages = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        try {
            while (System.currentTimeMillis() - startTime < maxWaitMillis) {
                long remainingTime = maxWaitMillis - (System.currentTimeMillis() - startTime);
                String message = messageQueue.poll(Math.max(1, remainingTime), TimeUnit.MILLISECONDS);
                
                if (message != null) {
                    messages.add(message);
                    // Try to get more messages without waiting
                    while ((message = messageQueue.poll()) != null) {
                        messages.add(message);
                    }
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Interrupted while reading messages");
        }
        
        return messages;
    }
    
    @Override
    public void teardown() {
        running = false;
        
        try {
            if (acceptThread != null && acceptThread.isAlive()) {
                acceptThread.interrupt();
                acceptThread.join(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.warning("Error closing server socket: " + e.getMessage());
        }
        
        logger.info("RELP server stopped");
    }
    
    @Override
    public String toString() {
        return "RelpInputItem" + System.lineSeparator() +
                "Port: " + port;
    }
}