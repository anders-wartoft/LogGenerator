package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.util.Configuration;

public abstract class AbstractInputItem implements InputItem {
    /** How many lines will be returned in one batch? */
    protected int batchSize;

    /**
     * Set the input batch size
     * @param config The configuration object to use
     */
    public AbstractInputItem(Configuration config) {
        batchSize = config.getInputBatchSize();
    }

}
