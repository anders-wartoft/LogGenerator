package nu.sitia.loggenerator.io;


import java.util.List;

public interface InputItem {

    /**
     * How many elements should be read at a time?
     * Default is 1.
     * @param size The new size
     */
    void setBatchSize(int size);

    /**
     * Let the item prepare for reading
     */
    void setup() throws RuntimeException;

    /**
     * Let the item teardown after reading
     */
    void teardown();

    /**
     * There are more messages to read
     * @return True iff there are more messages
     */
    boolean hasNext();

    /**
     * Read the next message
     * @return The next message
     */
    List<String> next();
}
