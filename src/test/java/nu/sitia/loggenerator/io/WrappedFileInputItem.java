package nu.sitia.loggenerator.io;

import nu.sitia.loggenerator.inputitems.FileInputItem;
import nu.sitia.loggenerator.util.CommandLineParser;

import java.util.ArrayList;
import java.util.List;

public class WrappedFileInputItem extends FileInputItem {
    private final List<String> receivedData = new ArrayList<>();
    /**
     * Create a new FileInputItem
     *
     * @param args The configuration that contains the filename
     */
    public WrappedFileInputItem(String [] args) {
        super(CommandLineParser.getCommandLineArgument(args, "in", "input-name", "Input file name"), args);
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
