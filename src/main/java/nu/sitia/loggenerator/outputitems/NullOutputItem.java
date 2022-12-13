package nu.sitia.loggenerator.outputitems;


import nu.sitia.loggenerator.util.Configuration;

import java.util.List;

public class NullOutputItem extends AbstractOutputItem implements SendListener {

    /**
     * Constructor. Add the callback method from this class.
     * @param config The Configuration object
     */
    public NullOutputItem(Configuration config) {
        super(config);
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
}
