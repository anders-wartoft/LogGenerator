package nu.sitia.loggenerator.outputitems;



import java.util.List;

public class NullOutputItem extends AbstractOutputItem implements SendListener {

    /**
     * Constructor. Add the callback method from this class.
     * @param args The command line arguments
     */
    public NullOutputItem(String [] args) {
        super(args);
        super.addListener(this);
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
     * Writes to the console.
     * @param toSend String to send
     */
    @Override
    public void send(List<String> toSend) {

    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "NullOutputItem";
    }
}
