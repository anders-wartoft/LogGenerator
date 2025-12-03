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

import junit.framework.TestCase;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

public class RelpOutputItemTest extends TestCase {
    static final Logger logger = Logger.getLogger(RelpOutputItemTest.class.getName());
    private RelpOutputItem relpOutputItem;
    private MockRelpServer server;
    private Thread serverThread;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        relpOutputItem = new RelpOutputItem(null);
        server = new MockRelpServer(10514);
        serverThread = new Thread(server);
        serverThread.start();
        // Give server time to start
        Thread.sleep(100);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (relpOutputItem != null) {
            try {
                relpOutputItem.teardown();
            } catch (Exception e) {
                // ignore
            }
        }
        server.stop();
        serverThread.join(1000);
    }
    
    /**
     * Test parameter setting
     */
    public void testSetParameter() {
        assertTrue(relpOutputItem.setParameter("--hostname", "localhost"));
        assertTrue(relpOutputItem.setParameter("-hn", "example.com"));
        assertTrue(relpOutputItem.setParameter("--port", "514"));
        assertTrue(relpOutputItem.setParameter("-p", "10514"));
        assertFalse(relpOutputItem.setParameter("--invalid", "value"));
    }
    
    /**
     * Test invalid port parameter
     */
    public void testInvalidPort() {
        try {
            relpOutputItem.setParameter("--port", "not-a-number");
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Port must be a valid integer"));
        }
    }
    
    /**
     * Test missing hostname
     */
    public void testMissingHostname() {
        relpOutputItem.setParameter("--port", "10514");
        try {
            relpOutputItem.afterPropertiesSet();
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Missing --hostname"));
        }
    }
    
    /**
     * Test missing port
     */
    public void testMissingPort() {
        relpOutputItem.setParameter("--hostname", "localhost");
        try {
            relpOutputItem.afterPropertiesSet();
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Port must be between"));
        }
    }
    
    /**
     * Test invalid port range
     */
    public void testInvalidPortRange() {
        relpOutputItem.setParameter("--hostname", "localhost");
        relpOutputItem.setParameter("--port", "70000");
        try {
            relpOutputItem.afterPropertiesSet();
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Port must be between"));
        }
    }
    
    /**
     * Test successful connection and send
     */
    public void testSuccessfulSend() throws Exception {
        relpOutputItem.setParameter("--hostname", "localhost");
        relpOutputItem.setParameter("--port", "10514");
        relpOutputItem.afterPropertiesSet();
        relpOutputItem.setup();
        
        List<String> messages = List.of("Test message 1", "Test message 2");
        relpOutputItem.send(messages);
        
        // Give server time to process
        Thread.sleep(100);
        
        assertTrue(server.getReceivedMessages().size() >= 2);
        assertTrue(server.getReceivedMessages().contains("Test message 1"));
        assertTrue(server.getReceivedMessages().contains("Test message 2"));
    }
    
    /**
     * Test toString
     */
    public void testToString() {
        relpOutputItem.setParameter("--hostname", "example.com");
        relpOutputItem.setParameter("--port", "514");
        String result = relpOutputItem.toString();
        assertTrue(result.contains("RelpOutputItem"));
        assertTrue(result.contains("example.com"));
        assertTrue(result.contains("514"));
    }
    
    /**
     * Mock RELP Server for testing
     */
    private static class MockRelpServer implements Runnable {
        private ServerSocket serverSocket;
        private volatile boolean running = true;
        private java.util.List<String> receivedMessages = new java.util.ArrayList<>();
        
        public MockRelpServer(int port) throws IOException {
            this.serverSocket = new ServerSocket(port);
        }
        
        @Override
        public void run() {
            try {
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    handleClient(clientSocket);
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
        
        private void handleClient(Socket clientSocket) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream output = clientSocket.getOutputStream();
            
            String line;
            while ((line = reader.readLine()) != null && running) {
                logger.fine("Mock server received: " + line);
                // Parse RELP frame: txnr command len data
                String[] parts = line.split(" ", 4);
                if (parts.length >= 3) {
                    String txnr = parts[0];
                    String command = parts[1];
                    
                    if ("open".equalsIgnoreCase(command)) {
                        // Send proper RELP response (single line, no embedded newlines)
                        String responseData = "RELP_VERSION=0";
                        String response = txnr + " rsp " + responseData.length() + " " + responseData + "\n";
                        output.write(response.getBytes());
                        output.flush();
                        logger.fine("Sent OPEN response: " + response);
                    } else if ("syslog".equalsIgnoreCase(command)) {
                        if (parts.length > 3) {
                            String data = parts[3];
                            receivedMessages.add(data);
                        }
                        String responseData = "OK";
                        String response = txnr + " rsp " + responseData.length() + " " + responseData + "\n";
                        output.write(response.getBytes());
                        output.flush();
                        logger.fine("Sent SYSLOG response");
                    } else if ("close".equalsIgnoreCase(command)) {
                        String responseData = "OK";
                        String response = txnr + " rsp " + responseData.length() + " " + responseData + "\n";
                        output.write(response.getBytes());
                        output.flush();
                        logger.fine("Sent CLOSE response");
                        break;
                    }
                }
            }
            
            reader.close();
            clientSocket.close();
        }
        
        public void stop() throws IOException {
            running = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
        
        public java.util.List<String> getReceivedMessages() {
            return receivedMessages;
        }
    }
}