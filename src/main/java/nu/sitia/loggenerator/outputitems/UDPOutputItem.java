package nu.sitia.loggenerator.outputitems;


import nu.sitia.loggenerator.util.CommandLineParser;

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

    /** Used in toString() */
    private final String hostName;

    /**
     * Constructor. Add the callback method from this class.
     * @param args The command line arguments
     */
    public UDPOutputItem(String [] args) {
        super(args);
        String hostName = CommandLineParser.getCommandLineArgument(args, "host", "host-name", "Host name to bind to");
        if (null == hostName) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("Parameter -host (--host-name) is required for UDP output item");
        }
        String portString = CommandLineParser.getCommandLineArgument(args, "port", "port", "Port to listen on");
        if (null == portString) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("Parameter -port (--port) is required for UDP output item");
        }
        port = Integer.parseInt(portString);

        super.addListener(this);
        try {
            address = InetAddress.getByName(hostName);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unknown host: " + hostName, e);
        }
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException("Socket exception", e);
        }
        this.hostName = hostName;
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

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "UDPOutputItem" + System.lineSeparator() +
                hostName + System.lineSeparator() +
                port;
    }
}
