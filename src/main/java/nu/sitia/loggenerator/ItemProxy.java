package nu.sitia.loggenerator;

import nu.sitia.loggenerator.filter.ProcessFilter;
import nu.sitia.loggenerator.inputitems.InputItem;
import nu.sitia.loggenerator.outputitems.OutputItem;
import nu.sitia.loggenerator.util.Configuration;
import nu.sitia.loggenerator.util.LogStatistics;
import sun.misc.Signal;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is the moderator that takes input, modifies the input and writes to the output.
 * The class is generic and can work with any input/output combination.
 */
public class ItemProxy {
    static final Logger logger = Logger.getLogger(ItemProxy.class.getName());

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

    /** Limit the number of events to send */
    private final long limit;

    /** The number of sent events (used with limit) */
    private long sentEvents;

    /** Keep track of sent events, start of transactions etc */
    private final LogStatistics statistics;

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
        this.limit = config.getLimit();
        this.sentEvents = 0;
        statistics = new LogStatistics(config);

        // Ctrl-C
        Signal.handle(new Signal("INT"),  // SIGINT
                signal -> {
                    System.out.println("Sigint");
                    if (config.isStatistics()) {
                        statistics.calculateStatistics(Configuration.END_TRANSACTION);
                    }
                    if (output.printTransactionMessages()) {
                        output.write(filterOutput(Configuration.END_TRANSACTION));
                    }
                    input.teardown();
                    output.teardown();
                    if (config.getDetector() != null) {
                        System.out.println(config.getDetector().toString());
                    }
                    System.exit(-1);
                });

    }

    /**
     * Main loop. Pump messages from the input, modify and then
     * write to the output. Batching may be done in the input and
     * output modules.
     * Teardown is called so that items may be able to handle
     * cached items.
     */
    public void pump() {
        logger.fine("ItemProxy starting up...");
        input.setup();
        output.setup();

        if (config.isStatistics()) {
            statistics.setTransactionStart(new Date().getTime());
        }
        if (output.printTransactionMessages()) {
            output.write(filterOutput(Configuration.BEGIN_TRANSACTION));
        }

        logger.finer("ItemProxy pumping messages...");
        // Grab inputs
        while (input.hasNext() && (limit == 0 || sentEvents < limit)) {
            // Assume we have no filters
            List<String> filtered = input.next();
            List<String> toSend = filterOutput(filtered);
            // in case of a batch of logs that will become more than the limit of logs
            while (limit != 0 && sentEvents + toSend.size() > limit) {
                toSend.remove(toSend.size()-1); // remove last entry
            }
            output.write(toSend);
            sentEvents += toSend.size();

            if (config.isStatistics()) {
                statistics.calculateStatistics(filtered);
            }
            // Should we throttle the output to lower the eps?
            throttle(statistics);
        }
        if (config.isStatistics()) {
            statistics.calculateStatistics(Configuration.END_TRANSACTION);
        }
        if (output.printTransactionMessages()) {
            output.write(filterOutput(Configuration.END_TRANSACTION));
        }
        input.teardown();
        output.teardown();
        if (config.getDetector() != null) {
            System.out.println(config.getDetector().toString());
        }
    }

    /**
     * ItemProxy want's to write transaction messages to the stream, but
     * the user might have added a filter for that. Run all filters
     * on that data
     * @param toFilter the data to filter
     * @return the filtered data (empty list or the argument)
     */
    private List<String> filterOutput(List<String> toFilter) {
        for (ProcessFilter filter : filterList) {
            // We had at least one filter. Process:
            toFilter = filter.filter(toFilter);
        }
        return toFilter;
    }

    /**
     * When eps is limited, throttle by using Thread.sleep()
     * @param statistics The Statistics to use to determine if we should throttle
     */
    private void throttle(LogStatistics statistics) {
        if (eps != 0 && config.isStatistics()) {
            long transactionStart = statistics.getTransactionStart();
            long sentMessages = statistics.getTransactionMessages();
            long now = new Date().getTime();
            // how long time should we spend on sending these messages?
            long estimatedTime = 1000 * sentMessages / eps;
            long waitTime = transactionStart + estimatedTime - now;
            if (waitTime > 10) {
                try {
                    Thread.sleep(waitTime - 10);
                } catch (InterruptedException e) {
                    // Ignore
                }
            } // end of throttling
        }
    }

}
