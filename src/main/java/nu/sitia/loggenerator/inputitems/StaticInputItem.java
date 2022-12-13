package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.util.Configuration;

import java.util.*;

/**
 * A StaticInputItem always return the same string. It's intended to
 * measure the performance of a system.
 */
public class StaticInputItem extends AbstractInputItem {
    /** The string to return */
    private final List<String> strings;

    /**
     * Create a new StaticInputItem
     * @param config The Configuration object
     */
    public StaticInputItem(Configuration config) {
        super(config);
        this.strings = List.of(config.getInputName());
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
        return strings;
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
        return this.strings.toString();
    }
}
