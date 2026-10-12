package nu.sitia.loggenerator.outputitems;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class TCPOutputItemTest extends TestCase {

    private TCPOutputItem createItem(int port, String reconnectTimeout) {
        TCPOutputItem item = new TCPOutputItem(null);
        item.setParameter("--hostname", "localhost");
        item.setParameter("--port", String.valueOf(port));
        item.setParameter("--reconnect-timeout", reconnectTimeout);
        item.setBatchSize(1);
        item.afterPropertiesSet();
        return item;
    }

    private static String readLine(Socket s) throws Exception {
        s.setSoTimeout(5000);
        return new BufferedReader(new InputStreamReader(s.getInputStream())).readLine();
    }

    /**
     * The receiver closes the connection (e.g., Logstash restart). The next send must
     * reconnect and deliver the event on the new connection.
     */
    public void testReconnectAfterPeerClose() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getByName("localhost"))) {
            TCPOutputItem item = createItem(server.getLocalPort(), "10");
            item.setup();
            try {
                Socket first = server.accept();
                item.write(List.of("one"));
                assertEquals("one", readLine(first));
                first.close();
                // Let the FIN reach the client
                Thread.sleep(200);

                item.write(List.of("two"));
                try (Socket second = server.accept()) {
                    assertEquals("two", readLine(second));
                }
                assertEquals(1, item.getReconnectCount());
            } finally {
                item.teardown();
            }
        }
    }

    /**
     * LogGenerator is started before the receiver. setup() must wait for the receiver.
     */
    public void testSetupWaitsForReceiver() throws Exception {
        int port;
        try (ServerSocket probe = new ServerSocket(0, 50, InetAddress.getByName("localhost"))) {
            port = probe.getLocalPort();
        }
        TCPOutputItem item = createItem(port, "10");
        final int p = port;
        Thread receiver = new Thread(() -> {
            try {
                Thread.sleep(1500);
                try (ServerSocket server = new ServerSocket(p, 50, InetAddress.getByName("localhost"));
                     Socket s = server.accept()) {
                    receivedLine = readLine(s);
                }
            } catch (Exception e) {
                receivedLine = "error: " + e;
            }
        });
        receiver.start();
        item.setup();
        item.write(List.of("hello"));
        receiver.join(5000);
        item.teardown();
        assertEquals("hello", receivedLine);
        assertEquals(0, item.getReconnectCount());
    }

    private volatile String receivedLine;

    /**
     * The receiver never shows up. setup() must give up after the reconnect timeout.
     */
    public void testSetupGivesUpWithoutReceiver() throws Exception {
        int port;
        try (ServerSocket probe = new ServerSocket(0, 50, InetAddress.getByName("localhost"))) {
            port = probe.getLocalPort();
        }
        TCPOutputItem item = createItem(port, "1");
        long start = System.currentTimeMillis();
        try {
            item.setup();
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Exception trying to connect"));
        }
        assertTrue(System.currentTimeMillis() - start < 5000);
    }

    /**
     * The receiver goes away and does not come back. The send must fail (not silently
     * succeed) so that the event isn't counted and the process can exit with an error.
     */
    public void testFailsWhenReceiverIsGone() throws Exception {
        TCPOutputItem item;
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getByName("localhost"))) {
            item = createItem(server.getLocalPort(), "1");
            item.setup();
            server.accept().close();
        }
        Thread.sleep(200);
        long start = System.currentTimeMillis();
        try {
            item.write(List.of("lost"));
            fail("Expected RuntimeException when the receiver is gone");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("unable to (re)connect"));
        }
        assertTrue("Should give up after the reconnect timeout", System.currentTimeMillis() - start < 5000);
        // Subsequent sends fail immediately
        try {
            item.write(List.of("lost again"));
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            item.teardown();
        } catch (RuntimeException e) {
            // expected, the unsent events are still cached
        }
    }
}
