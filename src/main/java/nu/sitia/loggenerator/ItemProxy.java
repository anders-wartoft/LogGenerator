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

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    /** Guards teardown/shutdown so they run exactly once across normal exit and the shutdown hook. */
    private final AtomicBoolean shutdownDone = new AtomicBoolean(false);

    /** Set by the shutdown hook so {@link #pump()} exits its loop promptly on SIGINT/SIGTERM. */
    private volatile boolean stopRequested = false;

    /** Set when all items have been set up. If setup fails there is no transaction to end. */
    private volatile boolean started = false;

    /**
     * The thread running {@link #pump()} (always the JVM main thread in practice). Recorded so
     * the shutdown hook thread (which the JVM runs concurrently with any still-running
     * application thread) can wait for pump() to actually finish before tearing items down —
     * without this, a shutdown hook that races a pump() loop still mid-batch would read the
     * (non-thread-safe) per-item state, e.g. GapDetector's duplicate map, half-updated.
     */
    private volatile Thread pumpThread;

    /**
     * Released when {@link #pump()} returns (normally or by exception). The shutdown hook waits
     * on this rather than joining {@link #pumpThread}, since the pump thread may itself call
     * System.exit() after pump() fails, and then never terminates while the hooks run.
     */
    private final CountDownLatch pumpDone = new CountDownLatch(1);

    /** Bounded wait for {@link #pumpThread} to quiesce before the shutdown hook tears down. */
    private static final long PUMP_JOIN_TIMEOUT_MS = 15_000;

    /**
     * Throttling baseline. -1 means "use the statistics transaction start". Moved to "now" when an
     * output reconnects, so that eps throttling continues at the configured rate instead of
     * bursting to catch up on the events that were not generated during the outage.
     */
    private long throttleStart = -1;

    /** Number of transaction messages already sent at {@link #throttleStart} */
    private long throttleMessageOffset = 0;

    /** Sum of the outputs' reconnect counts the last time we checked */
    private long lastReconnectCount = 0;

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

        // Covers SIGINT (Ctrl-C), SIGTERM (kill, systemd/k8s stop) and normal System.exit.
        //
        // IMPORTANT: the JVM runs shutdown hooks on their own thread(s) *concurrently* with any
        // other still-running application thread — it does not wait for pump()'s main thread to
        // quiesce first. So after flagging stopRequested, wait for pump() to return (bounded, in
        // case it's blocked somewhere that doesn't check stopRequested promptly) and let it reach
        // its own doShutdown(false) call cleanly. doShutdown() is idempotent (guarded by
        // shutdownDone), so if pump() wins the race this hook's own doShutdown(true) call below
        // becomes a no-op; if the wait times out (pump() truly stuck) we still fall through and
        // tear down from here so the process can exit.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received");
            stopRequested = true;
            Thread t = pumpThread;
            if (t != null && t != Thread.currentThread()) {
                try {
                    pumpDone.await(PUMP_JOIN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            doShutdown(true);
        }, "LogGenerator-shutdown"));
    }

    /**
     * Flush statistics, tear down items and run shutdown handlers. Idempotent: safe to call
     * from both the normal end of {@link #pump()} and the JVM shutdown hook.
     * @param interrupted true when invoked from the shutdown hook (SIGINT/SIGTERM path)
     */
    private void doShutdown(boolean interrupted) {
        if (!shutdownDone.compareAndSet(false, true)) {
            return;
        }
        // Each step is guarded so that one failing item (e.g., a lost TCP connection) does not
        // prevent the remaining items from being torn down.
        if (statistics != null && started) {
            statistics.calculateStatistics(Configuration.END_TRANSACTION);
            runQuietly("emit end of transaction", () -> emitMessage(Configuration.END_TRANSACTION_TEXT));
        }
        itemList.forEach(item -> runQuietly("teardown of " + item.getClass().getSimpleName(), item::teardown));
        shutdownHandlers.forEach(handler -> runQuietly("shutdown of " + handler.getClass().getSimpleName(), handler::shutdown));
    }

    private static void runQuietly(String what, Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            logger.warning("Failed " + what + ": " + e.getMessage());
        }
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
        pumpThread = Thread.currentThread();
        try {
            doPump();
        } catch (RuntimeException e) {
            // E.g., an output that has lost its connection. Tear down what we can and let the
            // caller exit with an error code so a process supervisor can restart us.
            doShutdown(false);
            throw e;
        } finally {
            // So the shutdown hook doesn't wait for a thread that is no longer pumping
            // (e.g., when the caller calls System.exit() after an error).
            pumpThread = null;
            pumpDone.countDown();
        }
    }

    private void doPump() {
        logger.fine("ItemProxy starting up...");
        itemList.forEach(item -> item.setup());
        started = true;

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
        while (!stopRequested && hasNext && (limit == 0 || sentEvents < limit) && (endTime == 0 || new Date().getTime() < endTime)) {
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
            resetThrottleOnReconnect(statistics);
            throttle(statistics);
            messages.clear();
        }
        doShutdown(false);
    }


    /**
     * If any output has reconnected since the last check, restart the throttling baseline.
     * @param statistics The Statistics used for throttling
     */
    private void resetThrottleOnReconnect(LogStatistics statistics) {
        long reconnects = 0;
        for (ProcessItem item : itemList) {
            if (OutputItem.class.isInstance(item)) {
                reconnects += ((OutputItem) item).getReconnectCount();
            }
        }
        if (reconnects != lastReconnectCount) {
            lastReconnectCount = reconnects;
            if (statistics != null) {
                throttleStart = new Date().getTime();
                throttleMessageOffset = statistics.getTransactionMessages();
                logger.fine("Output reconnected, restarting eps throttling baseline");
            }
        }
    }

    /**
     * When eps is limited, throttle by using Thread.sleep()
     * @param statistics The Statistics to use to determine if we should throttle
     */
    private void throttle(LogStatistics statistics) {
        if (eps != 0 && statistics != null) {
            long transactionStart = throttleStart >= 0 ? throttleStart : statistics.getTransactionStart();
            long sentMessages = statistics.getTransactionMessages() - throttleMessageOffset;
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
