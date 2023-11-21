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

package nu.sitia.loggenerator.util;

import nu.sitia.loggenerator.Configuration;

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
    private final long extraPrintoutEveryMs;

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

    /** Flag to be able to signal to the ItemProxy if statistics has been printed */
    private boolean hasPrinted = false;

    /**
     * Constructor
     * @param config Used to get the printout every ms value
     */
    public LogStatistics(Configuration config) {
        String printoutsString = config.getValue("-pp");

        if (printoutsString != null) {
            this.extraPrintoutEveryMs = Long.parseLong(printoutsString);
        } else {
            this.extraPrintoutEveryMs = 30000; // If not configured on the command line
        }
    }

    /**
     * Print statistics about this connection.
     * Inspect the data that is about to be sent for connection
     * messages.
     * @param filtered The list of strings to send
     * @return true iff the statistics have been printed out
     */
    public boolean calculateStatistics(List<String> filtered) {
        hasPrinted = false;
        filtered.forEach(this::checkMessage);
        return hasPrinted;
    }

    /**
     * A line may contain a lot of messages with \n delimiter
     * @param line The line to check
     */
    private void checkMessage(String line) {
        String [] messages = line.split("\n");
        for(String message: messages) {
            checkLog(message);
        }
    }

    /**
     * Check the message to be sent if there are any connection related data.
     * If so, print statistics about this connection.
     * @param log The message to inspect
     */
    private void checkLog(String log) {
        if (log.contains(Configuration.BEGIN_TRANSACTION_TEXT)) {
            transactionStart = new Date().getTime();
            transactionMessages = 0;
            transactionBytes = 0;
        } else if (log.contains(Configuration.BEGIN_FILE_TEXT)) {
            fileName = log.substring(Configuration.BEGIN_FILE_TEXT.length()
                + log.indexOf(Configuration.BEGIN_FILE_TEXT));
            fileTransferStart = new Date().getTime();
            fileMessages = 0;
            fileBytes = 0;
        } else if (log.contains(Configuration.END_TRANSACTION_TEXT)) {
            Date now = new Date();
            long elapsed = now.getTime() - transactionStart;
            printMetrics("Transaction", elapsed, transactionMessages, "", transactionBytes);
        } else if (log.contains(Configuration.END_FILE_TEXT)) {
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
        if (now.getTime() >= (lastPrintout + extraPrintoutEveryMs)) {
            long elapsed = now.getTime() - fileTransferStart;
            if (null != fileName)
                printMetrics("Transferring file", elapsed, fileMessages, fileName, fileBytes);
            printMetrics("Transaction transfer", now.getTime() - transactionStart, transactionMessages, "TRANSACTION", transactionBytes);
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
        hasPrinted = true;
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
