package nu.sitia.loggenerator.outputitems;


import nu.sitia.loggenerator.util.Configuration;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

public class UDPOutputItem extends AbstractOutputItem implements SendListener {
    static final Logger logger = Logger.getLogger(UDPOutputItem.class.getName());
    /** The InetAddress to connect to */
    private final InetAddress address;
    /** Port number to connect to */
    private final int port;
    /** The socket to use */
    private final DatagramSocket socket;

    /**
     * Constructor. Add the callback method from this class.
     * @param config The Configuration object
     */
    public UDPOutputItem(Configuration config) {
        super(config);
        super.addListener(this);
        // OutputName is host:port
        String [] temp = config.getOutputName().split(":");
        String hostname = temp[0];
        try {
            address = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unknown host: " + hostname, e);
        }
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException("Socket exception", e);
        }
        port = Integer.parseInt(temp[1]);
        addTransactionMessages = true;
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

    /**
     * Callback. What to do when the cache is full.
     * Writes to hostname:port
     * @param toSend String to send
     */
    @Override
    public void send(List<String> toSend) {
        for (String data : toSend) {
            byte[] buffer = data.getBytes(StandardCharsets.UTF_8);
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, port);
            try {
                logger.fine("Sending: " + toSend);
                socket.send(request);
                logger.finer("Sent message without exception");
            } catch (IOException e) {
                throw new RuntimeException("Socket send exception", e);
            }
        }
    }
}
