package nu.sitia.loggenerator.inputitems;


import java.util.List;

public interface InputItem {

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
