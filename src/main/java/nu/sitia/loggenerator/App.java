package nu.sitia.loggenerator;
import nu.sitia.loggenerator.filter.FilterListFactory;
import nu.sitia.loggenerator.filter.ProcessFilter;
import nu.sitia.loggenerator.inputitems.InputItem;
import nu.sitia.loggenerator.inputitems.InputItemFactory;
import nu.sitia.loggenerator.outputitems.OutputItem;
import nu.sitia.loggenerator.outputitems.OutputItemFactory;


import java.util.List;
import java.util.logging.Logger;

/**
 * Command line interface to the log generator
 *
 */
public class App 
{
    static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main( String[] args )
    {
        // Create a list of filters
        List<ProcessFilter> filterList = new FilterListFactory().create(args);

        // Now create the input and output items
        InputItem inputItem = InputItemFactory.create(args);
        OutputItem outputItem = OutputItemFactory.create(args);

        // And the proxy that mediates the flow
        ItemProxy proxy = new ItemProxy(inputItem, outputItem, filterList, args);

        // Print the configuration for every object
        logger.config(inputItem.toString());
        filterList.forEach(s -> logger.config(s.toString()));
        logger.config(outputItem.toString());

        // Start pumping messages
        proxy.pump();
    }
}
