package nu.sitia.loggenerator;

import nu.sitia.loggenerator.filter.ProcessFilter;
import nu.sitia.loggenerator.io.InputItem;
import nu.sitia.loggenerator.io.OutputItem;
import nu.sitia.loggenerator.util.Configuration;
import nu.sitia.loggenerator.util.LogStatistics;
import sun.misc.Signal;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is the moderator that takes input, modifies the input and writes to the output.
 * The class is generic and can work with any input/output combination.
 */
public class ItemProxy {
    static Logger logger = Logger.getLogger(ItemProxy.class.getName());

    /** The input item */
    private final InputItem input;
    /** The output item */
    private final OutputItem output;

    /** Filters to apply to each element processed */
    private final List<ProcessFilter> filterList;

    /** Config object */
    private final Configuration config;

    /** Preferred eps */
    private final long eps;

    /**
     * Default constructor
     * @param input Input to use
     * @param output Output to use
     */
    public ItemProxy(InputItem input, OutputItem output, List<ProcessFilter> filterList, Configuration config) {
        this.input = input;
        this.output = output;
        this.filterList = filterList;
        this.config = config;
        this.eps = config.getEps();
    }

    /**
     * Main loop. Pump messages from the input, modify and then
     * write to the output. Batching may be done in the input and
     * output modules.
     * Teardown is called so that items may be able to handle
     * cached items.
     */
    public void pump() {
        LogStatistics statistics = new LogStatistics();
        input.setup();
        output.setup();

        if (config.isStatistics()) {
            output.write(Configuration.BEGIN_TRANSACTION);
            // Ctrl-C
            Signal.handle(new Signal("INT"),  // SIGINT
                    signal -> {
                        output.write(Configuration.END_TRANSACTION);
                        System.exit(0);
                    });
        }


        // Grab inputs
        while (input.hasNext()) {
            // Assume we have no filters
            List<String> filtered = input.next();
            if (config.isStatistics()) {
                statistics.printStatistics(filtered);
            }
            for (ProcessFilter filter : filterList) {
                // We had at least one filter. Process:
                filtered = filter.filter(filtered);
            }
            output.write(filtered);
            // Should we throttle the output?
            if (eps != 0 && config.isStatistics()) {
                long transactionStart = statistics.getTransactionStart();
                long sentMessages = statistics.getTransactionMessages();
                long now = new Date().getTime();
                if (eps != 0) {
                    // how long time should we spend on sending these messages?
                    long estimatedTime = 1000 * sentMessages / eps;
                    long waitTime = transactionStart + estimatedTime - now;
                    if (waitTime > 10) {
                        try {
                            Thread.sleep(waitTime - 10);
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }
                } // end of throttling
            }
        }
        if (config.isStatistics()) {
            output.write(Configuration.END_TRANSACTION);
        }
        input.teardown();
        output.teardown();
    }

}
