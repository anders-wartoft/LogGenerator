package nu.sitia.loggenerator.io;


import java.util.ArrayList;
import java.util.List;

public class MemoryOutputItem extends AbstractOutputItem implements SendListener {
    private final List<String> receivedData = new ArrayList<>();
    /**
     * Constructor. Add the callback method from this class.
     */
    public MemoryOutputItem() {
        super();
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
        this.receivedData.addAll(toSend);
    }

    /**
     * Test usage
     * @return The internal data structure
     */
    public List getData() {
        return receivedData;
    }
}
