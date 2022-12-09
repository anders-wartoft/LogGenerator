package nu.sitia.loggenerator.io;

import nu.sitia.loggenerator.util.Configuration;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UDPInputItem extends AbstractInputItem {
    static Logger logger = Logger.getLogger(UDPInputItem.class.getName());
    /** The name:port to connect to */
    private final String hostName;
    /** The InetAddress to connect to */
    private InetAddress address;
    /** Port number to connect to */
    private int port;
    /** The socket to use */
    private DatagramSocket socket;

    /**
     * Create a new UDPInputItem
     * @param config The Configuration object
     */
    public UDPInputItem(Configuration config) {
        super();
        setBatchSize(config.getInputBatchSize());
        hostName = config.getInputName();
    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
        // InputName is host:port
        String [] temp = hostName.split(":");
        String hostname = temp[0];
        try {
            address = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unknown host: " + hostname, e);
        }
        try {
            port = Integer.valueOf(temp[1]);
            socket = new DatagramSocket();
            SocketAddress socketAddress = new InetSocketAddress(address, port);
            socket.bind(socketAddress);
        } catch (SocketException e) {
            throw new RuntimeException("Socket exception", e);
        }
        logger.info("Serving UDP server on port " + port);
    }

    /**
     * Are there more messages to read?
     * @return True iff there are more messages
     */
    public boolean hasNext() {
        return true;
    }

    /**
     * Read input one line at a time and return
     * {batchSize} elements.
     * @return The read input
     */
    public List<String> next() {
        List<String> result = new ArrayList<>();
        int lines = this.batchSize;
        while (lines-- > 0) {
            byte [] buffer = new byte[1024*1024];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(response);
                logger.log(Level.FINE, "Received connection from " + response.getAddress());
                String received = new String(buffer, 0, response.getLength());
                logger.log(Level.FINE, received);
                result.add(received);
            } catch (IOException e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Socket receive interrupted");
                }
            }
        }
        return result;
    }

    /**
     * Let the item teardown after reading
     */
    public void teardown() {
        // Unreachable code
    }
}
