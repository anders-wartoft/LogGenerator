package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.util.CommandLineParser;

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
     * @param args The command line arguments
     */
    public StaticInputItem(String [] args) {
        super(args);
        String string = CommandLineParser.getCommandLineArgument(args, "in", "input-name", "Input static name");
        if (null == string) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("-in argument is missing for -i static");
        }
        strings = List.of(string);
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
        return "StaticInputItem" + System.lineSeparator() + this.strings.toString();
    }
}
