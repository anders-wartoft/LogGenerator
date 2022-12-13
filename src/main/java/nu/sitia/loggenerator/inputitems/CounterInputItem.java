package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.util.Configuration;

import java.util.List;

/**
 * A CounterInputItem always return the same string but with an
 * ever-increasing number added at the end.
 */
public class CounterInputItem extends AbstractInputItem {
    /** The string to return */
    private final String string;

    /** The number to send */
    private long number = 1;

    /**
     * Create a new StaticInputItem
     * @param config The Configuration object
     */
    public CounterInputItem(Configuration config) {
        super(config);
        this.string = config.getInputName();
    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
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
        return List.of(string + number++);
    }

    /**
     * Let the item teardown after reading
     */
    public void teardown() {
    }

    /**
     * Debug printouts (logger)
     * @return The filename for this item
     */
    public String toString() {
        return this.string + this.number;
    }
}
