package nu.sitia.loggenerator.outputitems;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOutputItem implements OutputItem {
    /** How many lines will be returned in one batch? */
    protected int batchSize = 1;

    /** Callback method for sending the cache */
    private SendListener sl = null;

    /** If batchSize > 1, then store the elements in a cache until they can be written */
    private final List<String> cache = new ArrayList<>();

    /**
     * When the cache reaches batchSize numbers, or when the teardown is called,
     * the SendListener object will receive a call to the 'send()' method.
     * @param sl An object implementing the SendListener interface.
     */
    public void addListener (SendListener sl) {
        this.sl = sl;
    }

    public AbstractOutputItem() {
    }

    /**
     * How many elements should be read at a time?
     * Default is 1.
     * @param size The new size
     */
    public void setBatchSize(int size) {
        this.batchSize = size;
    }

    /**
     * Add one element to the cache. If the cache is full, call the
     * 'send()' method in the SendListener.
     * @param elements The element to add
     */
    public void write(List<String> elements) throws RuntimeException {
        // We must have a send method to call...
        assert(sl != null);
        cache.addAll(elements);
        if (cache.size() >= batchSize) {
            sl.send(cache);
            cache.clear();
        }
    }

    /**
     * Setup the environment.
     * @throws RuntimeException Not thrown here
     */
    @Override
    public void setup() throws RuntimeException {

    }

    /**
     * Tear down the environment.
     */
    @Override
    public void teardown() {
        if (!cache.isEmpty()) {
            sl.send(cache);
            cache.clear();
        }
    }

}
