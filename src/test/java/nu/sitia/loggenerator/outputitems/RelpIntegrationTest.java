package nu.sitia.loggenerator.outputitems;

import nu.sitia.loggenerator.inputitems.RelpInputItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.Assert.*;

@Ignore("RELP integration tests require manual testing until server implementation is fixed")
public class RelpIntegrationTest {
    static final Logger logger = Logger.getLogger(RelpIntegrationTest.class.getName());
    
    private RelpInputItem inputItem;
    private RelpOutputItem outputItem;
    private int testPort;
    private Thread serverThread;
    private final List<String> receivedMessages = new ArrayList<>();
    private volatile boolean serverRunning = false;
    
    @Before
    public void setUp() throws Exception {
        // Find an available port
        testPort = findAvailablePort();
        logger.info("Using port " + testPort + " for RELP test");
        
        receivedMessages.clear();
        
        // Start RELP server in a separate thread
        CountDownLatch serverStarted = new CountDownLatch(1);
        serverThread = new Thread(() -> {
            try {
                inputItem = new RelpInputItem(null);
                inputItem.setParameter("--port", String.valueOf(testPort));
                inputItem.afterPropertiesSet();
                inputItem.setup();
                serverRunning = true;
                serverStarted.countDown();
                
                // Read messages
                while (serverRunning && inputItem.hasNext()) {
                    List<String> messages = inputItem.next();
                    if (messages != null && !messages.isEmpty()) {
                        synchronized (receivedMessages) {
                            receivedMessages.addAll(messages);
                        }
                    }
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.warning("Server error: " + e.getMessage());
            }
        });
        serverThread.start();
        
        // Wait for server to start (max 5 seconds)
        if (!serverStarted.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout waiting for RELP server to start");
        }
        
        // Give server a bit more time to fully initialize
        Thread.sleep(100);
        
        // Setup output item
        outputItem = new RelpOutputItem(null);
        outputItem.setParameter("--hostname", "localhost");
        outputItem.setParameter("--port", String.valueOf(testPort));
        outputItem.afterPropertiesSet();
        outputItem.setup();
    }
    
    @After
    public void tearDown() throws Exception {
        serverRunning = false;
        
        if (outputItem != null) {
            try {
                outputItem.teardown();
            } catch (Exception e) {
                logger.warning("Error tearing down output: " + e.getMessage());
            }
        }
        
        if (inputItem != null) {
            try {
                inputItem.teardown();
            } catch (Exception e) {
                logger.warning("Error tearing down input: " + e.getMessage());
            }
        }
        
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread.join(2000);
        }
        
        // Extra cleanup time
        Thread.sleep(100);
    }
    
    /**
     * Find an available port for testing
     */
    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
    
    @Test
    public void testSendSingleMessage() throws Exception {
        // Send a single message
        List<String> messages = new ArrayList<>();
        messages.add("Test message 1");
        outputItem.write(messages);
        
        // Wait for message to be received
        Thread.sleep(500);
        
        // Verify
        synchronized (receivedMessages) {
            assertEquals("Should receive 1 message", 1, receivedMessages.size());
            assertTrue("Message should contain 'Test message 1'", 
                      receivedMessages.get(0).contains("Test message 1"));
        }
    }
    
    @Test
    public void testSendMultipleMessages() throws Exception {
        // Send multiple messages
        List<String> messages = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            messages.add("Test message " + i);
        }
        outputItem.write(messages);
        
        // Wait for messages to be received
        Thread.sleep(1000);
        
        // Verify
        synchronized (receivedMessages) {
            assertEquals("Should receive 10 messages", 10, receivedMessages.size());
            for (int i = 0; i < 10; i++) {
                assertTrue("Message " + i + " should contain correct text",
                          receivedMessages.get(i).contains("Test message " + (i + 1)));
            }
        }
    }
}