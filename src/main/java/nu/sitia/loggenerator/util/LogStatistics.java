package nu.sitia.loggenerator.util;

import java.util.Date;
import java.util.List;


public class LogStatistics {
    /** Current file in transfer */
    private String fileName;

    /** Current transaction start time */
    private long transactionStart;

    /** Current file transfer start time */
    private long fileTransferStart;

    /** How many messages in this transaction so far */
    private long transactionMessages;

    /** How many messages in this file so far */
    private long fileMessages;

    /** How many bytes have we transferred */
    private long transactionBytes;

    /** How many bytes have we transferred */
    private long fileBytes;

    /** Even if no end of file has been detected, do a printout every 30 second */
    private final long extraPrintoutEveryMs = 30000;

    /** When did we print statistics last? Set to creation time. */
    private long lastPrintout = new Date().getTime();


    public long getTransactionStart() {
        return transactionStart;
    }

    public void setTransactionStart(long transactionStart) {
        this.transactionStart = transactionStart;
    }

    public long getTransactionMessages() {
        return transactionMessages;
    }

    /**
     * Print statistics about this connection.
     * Inspect the data that is about to be sent for connection
     * messages.
     * @param filtered The list of strings to send
     */
    public void printStatistics(List<String> filtered) {
        filtered.forEach(this::checkLog);
    }

    /**
     * Check the message to be sent if there are any connection related data.
     * If so, print statistics about this connection.
     * @param log The message to inspect
     */
    private void checkLog(String log) {
        if (log.startsWith(Configuration.BEGIN_TRANSACTION_TEXT)) {
            transactionStart = new Date().getTime();
            transactionMessages = 0;
            transactionBytes = 0;
        } else if (log.startsWith(Configuration.BEGIN_FILE_TEXT)) {
            fileName = log.substring(Configuration.BEGIN_FILE_TEXT.length());
            fileTransferStart = new Date().getTime();
            fileMessages = 0;
            fileBytes = 0;
        } else if (log.startsWith(Configuration.END_TRANSACTION_TEXT)) {
            Date now = new Date();
            long elapsed = now.getTime() - transactionStart;
            printMetrics("Transaction", elapsed, transactionMessages, "", transactionBytes);
        } else if (log.startsWith(Configuration.END_FILE_TEXT)) {
            Date now = new Date();
            long elapsed = now.getTime() - fileTransferStart;
            printMetrics("File", elapsed, fileMessages, fileName, fileBytes);
        } else {
            fileMessages++;
            transactionMessages++;
            fileBytes += log.length();
            transactionBytes += log.length();
        }
        // Check if we should print for file again:
        Date now = new Date();
        if (now.getTime() >= lastPrintout + extraPrintoutEveryMs) {
            long elapsed = now.getTime() - fileTransferStart;
            printMetrics("Transferring file", elapsed, fileMessages, fileName, fileBytes);
        }
    }

    /**
     * Print some useful information on the receiving side if -s flag is used on the sending side
     * @param beginning Transaction or file
     * @param ms Time in ms for the event
     * @param nrMessages How many messages that has been received during that time
     * @param name Name of the file or empty
     * @param bytes How many characters the compound logs have contained (close enough to bytes received)
     */
    private void printMetrics(String beginning, long ms, long nrMessages, String name, long bytes) {
        System.out.format("%s: %s transferred %d lines in %d milliseconds, %.3f kEPS %.3f MBPS%n",
                beginning,
                name,
                nrMessages,
                ms,
                (nrMessages * 1.0/ms),
                (bytes * .008/ms));
        lastPrintout = new Date().getTime();
    }
}
