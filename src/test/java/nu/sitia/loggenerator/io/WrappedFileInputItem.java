package nu.sitia.loggenerator.io;

import nu.sitia.loggenerator.Configuration;
import nu.sitia.loggenerator.inputitems.FileInputItem;

import java.util.ArrayList;
import java.util.List;

public class WrappedFileInputItem extends FileInputItem {
    private final List<String> receivedData = new ArrayList<>();
    /**
     * Create a new FileInputItem
     *
     * @param config The configuration that contains the filename
     */
    public WrappedFileInputItem(Configuration config) {
        super(config.getValue("-ifn"), config);
    }

    @Override
    public List<String> next() {
        List<String> s = super.next();
        receivedData.addAll(s);
        return s;
    }


    /**
     *  Test usage
     *
     * @return The internal data structure
     */
    public List<String> getData() {
        return receivedData;
    }
}
