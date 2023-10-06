/*
 * Copyright 2022 sitia.nu https://github.com/anders-wartoft/LogGenerator
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nu.sitia.loggenerator;

import nu.sitia.loggenerator.filter.ProcessFilter;
import nu.sitia.loggenerator.inputitems.InputItem;
import nu.sitia.loggenerator.outputitems.OutputItem;
import nu.sitia.loggenerator.templates.Template;
import nu.sitia.loggenerator.templates.TemplateFactory;
import nu.sitia.loggenerator.templates.TimeTemplate;
import nu.sitia.loggenerator.util.LogStatistics;
import sun.misc.Signal;

import java.util.Date;
import java.util.LinkedList;
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

    /** Preferred eps */
    private final long eps;

    /** Limit the number of events to send */
    private final long limit;

    /** The number of sent events (used with limit) */
    private long sentEvents;

    /** Keep track of sent events, start of transactions etc */
    private final LogStatistics statistics;

    /** When exiting, traverse this list and call the shutdown handler */
    private final List<ShutdownHandler> shutdownHandlers = new LinkedList<>();

    /** If -t time:xxx, this is the start time + xxx */
    private long endTime = 0;

    /**
     * Default constructor
     * @param input Input to use
     * @param output Output to use
     * @param config The configuration
     */
    public ItemProxy(InputItem input, OutputItem output, List<ProcessFilter> filterList, Configuration config) {
        this.input = input;
        this.output = output;
        this.filterList = filterList;
        String isStatisticsString = config.getValue("-s");
        if (isStatisticsString != null && isStatisticsString.equalsIgnoreCase("true")) {
            statistics = new LogStatistics(config);
        } else {
            statistics = null;
        }

        String epsString = config.getValue("-e");
        if (null != epsString) {
            this.eps = Long.parseLong(epsString);
        } else {
            this.eps = 0;
        }

        String limitString = config.getValue("-l");
        if (null != limitString) {
            this.limit = Long.parseLong(limitString);
        } else {
            this.limit = 0;
        }

        String templateString = config.getValue("-t");

        if (templateString != null) {
            Template template = TemplateFactory.getTemplate(templateString);
            if (TimeTemplate.class.isInstance(template)) {
                endTime = ((TimeTemplate)template).getTime();
            }
        }

        // order might be important
        if (ShutdownHandler.class.isInstance(input)) {
            shutdownHandlers.add((ShutdownHandler) input);
        }
        shutdownHandlers.addAll(getShutdownHandlers(filterList));
        if (ShutdownHandler.class.isInstance(output)) {
            shutdownHandlers.add((ShutdownHandler) output);
        }

        this.sentEvents = 0;

        // Ctrl-C
        Signal.handle(new Signal("INT"),  // SIGINT
                signal -> {
                    System.out.println("Sigint");
                    if (statistics != null) {
                        statistics.calculateStatistics(Configuration.END_TRANSACTION);
                    }
                    if (output.printTransactionMessages()) {
                        output.write(filterOutput(Configuration.END_TRANSACTION));
                    }
                    input.teardown();
                    output.teardown();
                    shutdownHandlers.forEach(ShutdownHandler::shutdown);
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

        if (statistics != null) {
            statistics.setTransactionStart(new Date().getTime());
        }
        if (output.printTransactionMessages()) {
            output.write(filterOutput(Configuration.BEGIN_TRANSACTION));
        }

        logger.finer("ItemProxy pumping messages...");
        // Grab inputs as long as we have input and the limit is not reached and the time limit has not been reached
        while (input.hasNext() && (limit == 0 || sentEvents < limit) && (endTime == 0 || new Date().getTime() < endTime)) {
            // Assume we have no filters
            List<String> filtered = input.next();
            List<String> toSend = filterOutput(filtered);
            // in case of a batch of logs that will become more than the limit of logs
            while (limit != 0 && sentEvents + toSend.size() > limit) {
                toSend.remove(toSend.size()-1); // remove last entry
            }
            output.write(toSend);
            sentEvents += toSend.size();

            if (statistics != null) {
                statistics.calculateStatistics(filtered);
            }
            // Should we throttle the output to lower the eps?
            throttle(statistics);
        }
        if (statistics != null) {
            statistics.calculateStatistics(Configuration.END_TRANSACTION);
        }
        if (output.printTransactionMessages()) {
            output.write(filterOutput(Configuration.END_TRANSACTION));
        }
        input.teardown();
        output.teardown();
        shutdownHandlers.forEach(ShutdownHandler::shutdown);
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
        if (eps != 0 && statistics != null) {
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

    /**
     * Traverse the list of processFilters and see if any would like to be called
     * on exit.
     * @param processFilters All filters
     * @return A list of shutdown handlers
     */
    private List<ShutdownHandler> getShutdownHandlers(List<ProcessFilter> processFilters) {
        List<ShutdownHandler> shutdownHandlerList = new LinkedList<>();
        processFilters.forEach(s ->  {
            if (ShutdownHandler.class.isInstance(s)) {
                shutdownHandlerList.add((ShutdownHandler) s);
            }
        });
        return shutdownHandlerList;
    }

}
