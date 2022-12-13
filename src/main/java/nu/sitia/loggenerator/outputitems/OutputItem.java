package nu.sitia.loggenerator.outputitems;

import java.util.List;

public interface OutputItem {
    /**
     * When constructing an OutputItem the AbstractOutputItem will
     * call the "send()" method in SendListener when the cache
     * is full.
     * @param sl An object implementing the SendListener interface.
     */
    void addListener (SendListener sl);

    /**
     * Write elements batchSize elements at a time
     * @param element The element to write
     */
    void write(List<String> element) throws RuntimeException;

    /**
     * Let the item prepare for writing
     */
    void setup() throws RuntimeException;

    /**
     * Let the item teardown after writing
     */
    void teardown();

    /** Print transaction messages? */
     boolean printTransactionMessages();

}
