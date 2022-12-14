package nu.sitia.loggenerator.outputitems;

import java.util.Arrays;
import java.util.List;

public class CmdOutputItem extends AbstractOutputItem implements SendListener {

    /**
     * Constructor. Add the callback method from this class.
     * @param args The command line arguments
     */
    public CmdOutputItem(String [] args) {
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
        if (batchSize == 0) {
            // Call super.write to get batchSize events to the "send()" method here.
            // Ignore that and just output to the console with each line as a list
            // and if the line contains several \n then add each part as a list item
            if (!elements.isEmpty()) {
                elements.forEach(s -> System.out.println(Arrays.asList(s.split("\n"))));
            }
        } else {
            // super will call our send method with batchSize
            // or less number of events.
            super.write(elements);
        }
    }

    /**
     * Callback. What to do when the cache is full.
     * Writes to the console.
     * @param toSend String to send
     */
    @Override
    public void send(List<String> toSend) {
        System.out.println(toSend);
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "CmdOutputItem";
    }
}
