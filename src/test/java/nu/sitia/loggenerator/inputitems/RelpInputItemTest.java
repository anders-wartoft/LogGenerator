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

import junit.framework.TestCase;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class RelpInputItemTest extends TestCase {
    
    private RelpInputItem relpInputItem;
    private static final int TEST_PORT = 10515;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        relpInputItem = new RelpInputItem(null);
        relpInputItem.setParameter("--port", String.valueOf(TEST_PORT));
        relpInputItem.afterPropertiesSet();
        relpInputItem.setup();
        // Give server time to start
        Thread.sleep(100);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (relpInputItem != null) {
            relpInputItem.teardown();
        }
    }
    
    /**
     * Test parameter setting
     */
    public void testSetParameter() {
        RelpInputItem item = new RelpInputItem(null);
        assertTrue(item.setParameter("--port", "514"));
        assertTrue(item.setParameter("-p", "10515"));
        assertFalse(item.setParameter("--invalid", "value"));
    }
    
    /**
     * Test invalid port parameter
     */
    public void testInvalidPort() {
        RelpInputItem item = new RelpInputItem(null);
        try {
            item.setParameter("--port", "not-a-number");
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Port must be a valid integer"));
        }
    }
    
    /**
     * Test invalid port range
     */
    public void testInvalidPortRange() {
        RelpInputItem item = new RelpInputItem(null);
        item.setParameter("--port", "70000");
        try {
            item.afterPropertiesSet();
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Port must be between"));
        }
    }
    
    /**
     * Test zero port
     */
    public void testZeroPort() {
        RelpInputItem item = new RelpInputItem(null);
        item.setParameter("--port", "0");
        try {
            item.afterPropertiesSet();
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Port must be between"));
        }
    }
    
    /**
     * Test receiving a single message
     */
    public void testReceiveSingleMessage() throws Exception {
        sendRelpMessage("Test message 1");
        
        List<String> messages = relpInputItem.next();
        
        assertEquals(1, messages.size());
        assertEquals("Test message 1", messages.get(0));
    }
    
/**
 * Test receiving multiple messages
 */
public void testReceiveMultipleMessages() throws Exception {
    sendRelpMessage("Message 1");
    sendRelpMessage("Message 2");
    sendRelpMessage("Message 3");
    
    List<String> allMessages = new java.util.ArrayList<>();
    long startTime = System.currentTimeMillis();
    
    // Poll until we get 3 messages or timeout
    while (allMessages.size() < 3 && System.currentTimeMillis() - startTime < 2000) {
        List<String> messages = relpInputItem.next();
        allMessages.addAll(messages);
        if (allMessages.size() < 3) {
            Thread.sleep(100);
        }
    }
    
    assertEquals(3, allMessages.size());
    assertTrue(allMessages.contains("Message 1"));
    assertTrue(allMessages.contains("Message 2"));
    assertTrue(allMessages.contains("Message 3"));
}
    
    /**
     * Test receiving message with special characters
     */
    public void testReceiveMessageWithSpecialChars() throws Exception {
        String specialMessage = "Special chars: \"quotes\", \\backslash\\, \ttab";
        sendRelpMessage(specialMessage);
        
        List<String> messages = relpInputItem.next();
        
        assertEquals(1, messages.size());
        assertEquals(specialMessage, messages.get(0));
    }
    
    /**
     * Test timeout with no messages
     */
    public void testTimeoutNoMessages() throws Exception {
        long startTime = System.currentTimeMillis();
        List<String> messages = relpInputItem.next();
        long elapsed = System.currentTimeMillis() - startTime;
        
        assertEquals(0, messages.size());
        assertTrue(elapsed >= 100);
        assertTrue(elapsed < 2000);
    }
    
    /**
     * Test toString
     */
    public void testToString() {
        String result = relpInputItem.toString();
        assertTrue(result.contains("RelpInputItem"));
        assertTrue(result.contains("Port"));
        assertTrue(result.contains(String.valueOf(TEST_PORT)));
    }
    
    /**
     * Test RELP open/close handshake
     */
    public void testRelpHandshake() throws Exception {
        Socket socket = new Socket("localhost", TEST_PORT);
        OutputStream output = socket.getOutputStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        // Send OPEN
        String openFrame = "1 open 0 \n";
        output.write(openFrame.getBytes());
        output.flush();
        
        String response = input.readLine();
        assertNotNull(response);
        assertTrue(response.contains("rsp"));
        assertTrue(response.contains("OK"));
        
        // Send CLOSE
        String closeFrame = "2 close 0 \n";
        output.write(closeFrame.getBytes());
        output.flush();
        
        response = input.readLine();
        assertNotNull(response);
        assertTrue(response.contains("rsp"));
        
        input.close();
        socket.close();
    }
    
    /**
     * Test receiving large message
     */
    public void testReceiveLargeMessage() throws Exception {
        String largeMessage = "A".repeat(10000);
        sendRelpMessage(largeMessage);
        
        List<String> messages = relpInputItem.next();
        
        assertEquals(1, messages.size());
        assertEquals(largeMessage, messages.get(0));
    }
    
    /**
     * Test receiving message with newlines (edge case)
     */
    public void testReceiveMessageWithNewlines() throws Exception {
        // Note: In RELP, newlines in data would need to be properly handled
        String messageWithNewlines = "Line 1\\nLine 2";
        sendRelpMessage(messageWithNewlines);
        
        List<String> messages = relpInputItem.next();
        
        assertEquals(1, messages.size());
        assertEquals(messageWithNewlines, messages.get(0));
    }
    
    /**
     * Helper method to send a RELP message to the server
     */
    private void sendRelpMessage(String message) throws Exception {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", TEST_PORT);
                OutputStream output = socket.getOutputStream();
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // Send OPEN
                String openFrame = "1 open 0 \n";
                output.write(openFrame.getBytes());
                output.flush();
                input.readLine(); // Read response
                
                // Send SYSLOG
                String syslogFrame = "2 syslog " + message.length() + " " + message + "\n";
                output.write(syslogFrame.getBytes());
                output.flush();
                input.readLine(); // Read response
                
                // Send CLOSE
                String closeFrame = "3 close 0 \n";
                output.write(closeFrame.getBytes());
                output.flush();
                input.readLine(); // Read response
                
                input.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}