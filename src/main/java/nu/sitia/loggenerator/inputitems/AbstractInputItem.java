package nu.sitia.loggenerator.inputitems;

public abstract class AbstractInputItem implements InputItem {
    /** How many lines will be returned in one batch? */
    protected int batchSize = 1;

    /**
     * How many elements should be read at a time?
     * Default is 1.
     * @param size The new size
     */
    public void setBatchSize(int size) {
        this.batchSize = size;
    }

}
