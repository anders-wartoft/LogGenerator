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

import nu.sitia.loggenerator.inputitems.RelpInputItem;
import junit.framework.TestCase;
import java.util.List;

public class RelpIntegrationTest extends TestCase {
    
    private RelpOutputItem relpOutputItem;
    private RelpInputItem relpInputItem;
    private static final int TEST_PORT = 10516;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Setup input item (server)
        relpInputItem = new RelpInputItem(null);
        relpInputItem.setParameter("--port", String.valueOf(TEST_PORT));
        relpInputItem.afterPropertiesSet();
        relpInputItem.setup();
        
        // Give server time to start
        Thread.sleep(200);
        
        // Setup output item (client)
        relpOutputItem = new RelpOutputItem(null);
        relpOutputItem.setParameter("--hostname", "localhost");
        relpOutputItem.setParameter("--port", String.valueOf(TEST_PORT));
        relpOutputItem.afterPropertiesSet();
        relpOutputItem.setup();
        
        // Give connection time to establish
        Thread.sleep(200);
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
        if (relpInputItem != null) {
            relpInputItem.teardown();
        }
    }
    
    /**
     * Test sending and receiving a single message
     */
    public void testSendAndReceiveSingleMessage() throws Exception {
        String testMessage = "Test message from output to input";
        
        // Send message
        relpOutputItem.send(List.of(testMessage));
        
        // Give message time to travel
        Thread.sleep(300);
        
        // Receive message
        List<String> receivedMessages = relpInputItem.next();
        
        assertEquals(1, receivedMessages.size());
        assertEquals(testMessage, receivedMessages.get(0));
    }
    
    /**
     * Test sending and receiving multiple messages
     */
    public void testSendAndReceiveMultipleMessages() throws Exception {
        List<String> messagesToSend = List.of(
                "Message 1",
                "Message 2",
                "Message 3"
        );
        
        // Send messages
        relpOutputItem.send(messagesToSend);
        
        // Give messages time to travel
        Thread.sleep(500);
        
        // Receive messages
        List<String> allReceivedMessages = new java.util.ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        // Poll until we get all 3 messages or timeout
        while (allReceivedMessages.size() < 3 && System.currentTimeMillis() - startTime < 2000) {
            List<String> messages = relpInputItem.next();
            allReceivedMessages.addAll(messages);
            if (allReceivedMessages.size() < 3) {
                Thread.sleep(100);
            }
        }
        
        assertEquals(3, allReceivedMessages.size());
        assertTrue(allReceivedMessages.contains("Message 1"));
        assertTrue(allReceivedMessages.contains("Message 2"));
        assertTrue(allReceivedMessages.contains("Message 3"));
    }
    
    /**
     * Test sending and receiving JSON message
     */
    public void testSendAndReceiveJsonMessage() throws Exception {
        String jsonMessage = "{\"level\":\"INFO\",\"message\":\"Test event\",\"timestamp\":\"2025-12-03T10:00:00Z\"}";
        
        // Send message
        relpOutputItem.send(List.of(jsonMessage));
        
        // Give message time to travel
        Thread.sleep(300);
        
        // Receive message
        List<String> receivedMessages = relpInputItem.next();
        
        assertEquals(1, receivedMessages.size());
        assertEquals(jsonMessage, receivedMessages.get(0));
    }
    
    /**
     * Test sending and receiving message with special characters
     */
    public void testSendAndReceiveMessageWithSpecialChars() throws Exception {
        String specialMessage = "Message with special chars: \"quoted\", \\backslash\\, tab\there";
        
        // Send message
        relpOutputItem.send(List.of(specialMessage));
        
        // Give message time to travel
        Thread.sleep(300);
        
        // Receive message
        List<String> receivedMessages = relpInputItem.next();
        
        assertEquals(1, receivedMessages.size());
        assertEquals(specialMessage, receivedMessages.get(0));
    }
    
    /**
     * Test sending and receiving large message
     */
    public void testSendAndReceiveLargeMessage() throws Exception {
        String largeMessage = "A".repeat(5000);
        
        // Send message
        relpOutputItem.send(List.of(largeMessage));
        
        // Give message time to travel
        Thread.sleep(300);
        
        // Receive message
        List<String> receivedMessages = relpInputItem.next();
        
        assertEquals(1, receivedMessages.size());
        assertEquals(largeMessage, receivedMessages.get(0));
    }
    
    /**
     * Test sending and receiving messages with timestamps
     */
    public void testSendAndReceiveMessagesWithTimestamps() throws Exception {
        List<String> messagesToSend = List.of(
                "2025-12-03T10:00:00Z - Event 1",
                "2025-12-03T10:00:01Z - Event 2",
                "2025-12-03T10:00:02Z - Event 3"
        );
        
        // Send messages
        relpOutputItem.send(messagesToSend);
        
        // Give messages time to travel
        Thread.sleep(500);
        
        // Receive messages
        List<String> allReceivedMessages = new java.util.ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        while (allReceivedMessages.size() < 3 && System.currentTimeMillis() - startTime < 2000) {
            List<String> messages = relpInputItem.next();
            allReceivedMessages.addAll(messages);
            if (allReceivedMessages.size() < 3) {
                Thread.sleep(100);
            }
        }
        
        assertEquals(3, allReceivedMessages.size());
        assertTrue(allReceivedMessages.contains("2025-12-03T10:00:00Z - Event 1"));
        assertTrue(allReceivedMessages.contains("2025-12-03T10:00:01Z - Event 2"));
        assertTrue(allReceivedMessages.contains("2025-12-03T10:00:02Z - Event 3"));
    }
    
    /**
     * Test sending empty list
     */
    public void testSendEmptyList() throws Exception {
        // Send empty list
        relpOutputItem.send(List.of());
        
        // Give time to process
        Thread.sleep(300);
        
        // Should not receive anything
        List<String> receivedMessages = relpInputItem.next();
        assertEquals(0, receivedMessages.size());
    }
    
    /**
     * Test sending and receiving syslog format message
     */
    public void testSendAndReceiveSyslogMessage() throws Exception {
        String syslogMessage = "<134>Dec  3 10:00:00 hostname application[1234]: This is a syslog message";
        
        // Send message
        relpOutputItem.send(List.of(syslogMessage));
        
        // Give message time to travel
        Thread.sleep(300);
        
        // Receive message
        List<String> receivedMessages = relpInputItem.next();
        
        assertEquals(1, receivedMessages.size());
        assertEquals(syslogMessage, receivedMessages.get(0));
    }
    
    /**
     * Test rapid send and receive
     */
    public void testRapidSendAndReceive() throws Exception {
        for (int i = 0; i < 10; i++) {
            String message = "Rapid message " + i;
            relpOutputItem.send(List.of(message));
            Thread.sleep(50);
        }
        
        // Give time for all messages to arrive
        Thread.sleep(500);
        
        // Receive all messages
        List<String> allReceivedMessages = new java.util.ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        while (allReceivedMessages.size() < 10 && System.currentTimeMillis() - startTime < 3000) {
            List<String> messages = relpInputItem.next();
            allReceivedMessages.addAll(messages);
            if (allReceivedMessages.size() < 10) {
                Thread.sleep(100);
            }
        }
        
        assertEquals(10, allReceivedMessages.size());
        for (int i = 0; i < 10; i++) {
            assertTrue(allReceivedMessages.contains("Rapid message " + i));
        }
    }
    
    /**
     * Test that output and input can be connected and disconnected gracefully
     */
    public void testGracefulConnectDisconnect() throws Exception {
        // Already connected in setUp
        assertNotNull(relpOutputItem);
        assertNotNull(relpInputItem);
        
        // Send a test message
        relpOutputItem.send(List.of("Connection test"));
        Thread.sleep(300);
        
        // Receive it
        List<String> messages = relpInputItem.next();
        assertEquals(1, messages.size());
        
        // Disconnect
        relpOutputItem.teardown();
        
        // Verify we can reconnect
        relpOutputItem = new RelpOutputItem(null);
        relpOutputItem.setParameter("--hostname", "localhost");
        relpOutputItem.setParameter("--port", String.valueOf(TEST_PORT));
        relpOutputItem.afterPropertiesSet();
        relpOutputItem.setup();
        
        Thread.sleep(200);
        
        // Send and receive again
        relpOutputItem.send(List.of("Reconnection test"));
        Thread.sleep(300);
        
        messages = relpInputItem.next();
        assertTrue(messages.size() >= 1);
        assertTrue(messages.contains("Reconnection test"));
    }
}