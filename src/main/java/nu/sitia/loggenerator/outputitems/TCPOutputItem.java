package nu.sitia.loggenerator.outputitems;


import nu.sitia.loggenerator.util.CommandLineParser;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.List;
import java.util.logging.Logger;

public class TCPOutputItem extends AbstractOutputItem implements SendListener {
    static final Logger logger = Logger.getLogger(TCPOutputItem.class.getName());
    /** The address to use */
    private final String hostName;
    /** The port to connect to */
    private final int port;
    /** The socket to use */
    private Socket socket;

    /**
     * Constructor. Add the callback method from this class.
     * @param args The command line arguments
     */
    public TCPOutputItem(String [] args) {
        super(args);
        super.addListener(this);
        hostName = CommandLineParser.getCommandLineArgument(args, "host", "host-name", "Host name to bind to");
        if (null == hostName) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("Parameter -host (--host-name) is required for TCP output item");
        }
        String portString = CommandLineParser.getCommandLineArgument(args, "port", "port", "Port to listen on");
        if (null == portString) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("Parameter -port (--port) is required for TCP output item");
        }
        port = Integer.parseInt(portString);
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
        final boolean autoFlush = true;
        try {
            OutputStream output = socket.getOutputStream();
            logger.fine("Sending: " + toSend);
            PrintWriter writer = new PrintWriter(output, autoFlush);
            toSend.forEach(writer::println);
            logger.finer("Sent message without exception");
        } catch (IOException e) {
            throw new RuntimeException("Socket send exception", e);
        }
    }

    @Override
    public void setup() throws RuntimeException {
        super.setup();
        // OutputName is host:port
        try {
            socket = new Socket(hostName, port);
        } catch (IOException e) {
            throw new RuntimeException("Socket exception", e);
        }
    }

    @Override
    public void teardown() {
        super.teardown();
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException("Socket close exception", e);
        }
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "TCPOutputItem" + System.lineSeparator() +
                hostName + System.lineSeparator() +
                port;
    }
}
