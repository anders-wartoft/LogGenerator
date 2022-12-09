package nu.sitia.loggenerator.io;


import nu.sitia.loggenerator.util.Configuration;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.List;
import java.util.logging.Logger;

public class TCPOutputItem extends AbstractOutputItem implements SendListener {
    static Logger logger = Logger.getLogger(TCPOutputItem.class.getName());
    /** The address:port to use */
    private final String hostName;
    /** The socket to use */
    private Socket socket;

    /**
     * Constructor. Add the callback method from this class.
     * @param config The Configuration object
     */
    public TCPOutputItem(Configuration config) {
        super();
        super.addListener(this);
        hostName = config.getOutputName();
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
        String [] temp = hostName.split(":");
        String hostname = temp[0];
        try {
            int port = Integer.parseInt(temp[1]);
            socket = new Socket(hostname, port);
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
}
