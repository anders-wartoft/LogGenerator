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
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nu.sitia.loggenerator;

import nu.sitia.loggenerator.filter.GapDetectionFilter;
import nu.sitia.loggenerator.filter.ProcessFilter;
import nu.sitia.loggenerator.inputitems.InputItem;
import nu.sitia.loggenerator.inputitems.TemplateFileInputItem;
import nu.sitia.loggenerator.outputitems.OutputItem;
import nu.sitia.loggenerator.templates.Template;
import nu.sitia.loggenerator.templates.TimeTemplate;
import nu.sitia.loggenerator.util.LogStatistics;
import sun.misc.Signal;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This is the moderator that takes input, modifies the input and writes to the output.
 * The class is generic and can work with any input/output combination.
 */
public class ItemProxy {
    static final Logger logger = Logger.getLogger(ItemProxy.class.getName());

    /** Filters to apply to each element processed */
    private final List<ProcessItem> itemList;

    /** Preferred eps */
    private final double eps;

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

    /** If we have a gapDetector and the flag -cgd is true, then show
     * gaps every time the statistics has been printed.
     */
    private List<ProcessItem> gapDetectors;

    private void emitMessage(String message) {
        List<String> result = Arrays.asList(message);
        for (ProcessItem item : itemList) {
            if (ProcessFilter.class.isInstance(item)) {
                ProcessFilter filter = (ProcessFilter) item;
                result = filter.filter(result);
            } else if (OutputItem.class.isInstance(item)) {
                OutputItem output = (OutputItem) item;
                output.write(result);
            }
        }
    }

    /**
     * Default constructor
     * @param itemList A list of inputs, filters and outputs
     * @param config The configuration
     */
    public ItemProxy(List<ProcessItem> itemList, Configuration config) {
        this.itemList = itemList;
        if (config.isStatistics()) {
            statistics = new LogStatistics(config);
        } else {
            statistics = null;
        }

        this.eps = config.getEps();

        this.limit = config.getLimit();

        List<ProcessItem> templates =
                itemList.stream().filter(item -> TemplateFileInputItem.class.isInstance(item)).collect(Collectors.toList());
        long tempTime = -1;
        for (ProcessItem item : templates) {
            TemplateFileInputItem templateFileInputItem = (TemplateFileInputItem) item;
            Template template = templateFileInputItem.getTemplate();
            if (TimeTemplate.class.isInstance(template)) {
                TimeTemplate tt = (TimeTemplate) template;
                tempTime = tempTime > tt.getTime() || tempTime == -1 ? tt.getTime() : tempTime;
            }
        }
        if (tempTime != -1) {
            endTime = tempTime;
        }

        itemList.forEach(item -> shutdownHandlers.add((ShutdownHandler) item));

        gapDetectors = getGapDetectors(itemList);

        this.sentEvents = 0;

        // Ctrl-C
        Signal.handle(new Signal("INT"),  // SIGINT
                signal -> {
                    System.out.println("Sigint");
                    if (this.statistics != null) {
                        statistics.calculateStatistics(Configuration.END_TRANSACTION);
                        // Send end message (maybe filtered)
                        emitMessage(Configuration.END_TRANSACTION_TEXT);
                    }
                    itemList.forEach(item -> item.teardown());
                    shutdownHandlers.forEach(ShutdownHandler::shutdown);
                    System.exit(-1);
                });

    }


    /**
     * Search the list of filters and, if present, return a GapDetectionFilter
     * @param filterList a List<filter> to search
     * @return GapDetectionFilter or null
     */
    private List<ProcessItem> getGapDetectors(List<ProcessItem>filterList) {
        List<ProcessItem> result = new ArrayList<>();
        filterList.stream().forEach(f -> {
            if (GapDetectionFilter.class.isInstance(f)) {
                result.add(f);
            }
        });
        return result;
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
        itemList.forEach(item -> item.setup());

        if (statistics != null) {
            statistics.setTransactionStart(new Date().getTime());
            logger.finest("Transaction start: " + statistics.getTransactionStart());
        }
        List<String> messages = new ArrayList<>();

        // When we don't have any more input items that report hasNext true,
        // then we are done.
        boolean hasNext = true;
        // If we have a statistics object, then we want to send a start message
        boolean firstTime = true;
        logger.finer("ItemProxy pumping messages...");
        // Grab inputs as long as we have input and the limit is not reached and the time limit has not been reached
        while (hasNext && (limit == 0 || sentEvents < limit) && (endTime == 0 || new Date().getTime() < endTime)) {
            hasNext = false;
            for (ProcessItem item : itemList) {
                if (InputItem.class.isInstance(item)) {
                    InputItem input = (InputItem) item;
                    if (input.hasNext()) {
                        hasNext = true;
                        List<String> next = input.next();
                        messages.addAll(next);
                    }
                } else if (OutputItem.class.isInstance(item)) {
                    OutputItem output = (OutputItem) item;
                    output.write(messages);
                } else if (ProcessFilter.class.isInstance(item)) {
                    ProcessFilter filter = (ProcessFilter) item;
                    messages = filter.filter(messages);
                }
                if (firstTime && statistics != null && messages.size() > 0) {
                    // Send start message (maybe filtered)
                    emitMessage(Configuration.BEGIN_TRANSACTION_TEXT);
                    firstTime = false;
                }
            }
            sentEvents += messages.size();
            if (statistics != null) {
                boolean hasPrinted = statistics.calculateStatistics(messages);
                if (hasPrinted && gapDetectors.size() > 0) {
                    // Also, print the gapDetection periodically
                    gapDetectors.forEach(gapDetector -> {
                        System.out.println(((GapDetectionFilter)gapDetector).getDetector().toString());
                    });
                }
            }
            // Should we throttle the output to lower the eps?
            throttle(statistics);
            messages.clear();
        }
        if (statistics != null) {
            emitMessage(Configuration.END_TRANSACTION_TEXT);
            statistics.calculateStatistics(Configuration.END_TRANSACTION);
        }
        itemList.forEach(item -> item.teardown());
        shutdownHandlers.forEach(ShutdownHandler::shutdown);
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
            long estimatedTime = (long)(1000 * sentMessages / eps);
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
