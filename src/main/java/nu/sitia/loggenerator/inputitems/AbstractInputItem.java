package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.util.CommandLineParser;

public abstract class AbstractInputItem implements InputItem {
    /** How many lines will be returned in one batch? */
    protected int batchSize;

    /**
     * Set the input batch size
     * @param args The command line configuration object to use
     */
    public AbstractInputItem(String [] args) {
        String batchString = CommandLineParser.getCommandLineArgument(args, "ib", "input-batch-size", "How many rows to read before sending to processing");
        if (null != batchString) {
            batchSize = Integer.parseInt(batchString);
        } else {
            batchSize = 1; // Default value
        }
    }

}
