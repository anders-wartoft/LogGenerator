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

package nu.sitia.loggenerator.filter;

import nu.sitia.loggenerator.Configuration;
import nu.sitia.loggenerator.ShutdownHandler;
import nu.sitia.loggenerator.inputitems.UDPInputItem;
import nu.sitia.loggenerator.util.gapdetector.GapDetector;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GapDetectionFilter extends AbstractProcessFilter {
    static final Logger logger = Logger.getLogger(GapDetectionFilter.class.getName());

    /** The detector */
    private final GapDetector detector = new GapDetector();

    /** The pattern to get the next number from the log */
    private Pattern pattern;

    /** Used in toString() */
    private String regex;

    /** Printable or JSON report */
    private boolean jsonReport;

    /** Should we also detect duplicates? */
    private boolean duplicatesDetection = false;

    /** Should we report gaps continuously? */
    private boolean isContinuousGapDetection;

    /**
     * Create a guardFilter and set all parameters
     * @param config The configuration
     */
    public GapDetectionFilter(Configuration config) {
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("GapDetectionFilter. Detect gaps in a sequence of numbers\n" +
                    "Parameters:\n" +
                    "--regex <regex> (-r <regex>)\n" +
                    "  The regex to match\n" +
                    "--duplicate-detection <true|false> (-dd <true|false>)\n" +
                    "  Should we also detect duplicates?\n" +
                    "--json <true|false> (-j <true|false>)\n" +
                    "  Should we print the result in JSON format?\n" +
                    "--continuous <true|false> (-c <true|false>)\n" +
                    "  Should we report gaps continuously?\n");
            System.exit(1);
        }
        if (key != null && (key.equalsIgnoreCase("--regex") || key.equalsIgnoreCase("-r"))) {
            this.regex = value;
            logger.fine("regex " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--duplicate-detection") || key.equalsIgnoreCase("-dd"))) {
            this.duplicatesDetection = value.equalsIgnoreCase("true");
            logger.fine("duplicatesDetection " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--json") || key.equalsIgnoreCase("-j"))) {
            this.jsonReport = value.equalsIgnoreCase("true");
            logger.fine("json " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--continuous") || key.equalsIgnoreCase("-c"))) {
            this.isContinuousGapDetection = value.equalsIgnoreCase("true");
            logger.fine("isContinuousGapDetection " + value);
            return true;
        }
        return false;
    }

    @Override
    public boolean afterPropertiesSet() {
        if (regex == null) {
            throw new RuntimeException("Missing --regex parameter");
        }
        pattern = Pattern.compile(regex);
        detector.setDuplicateDetection(duplicatesDetection);
        return true;
    }

    /**
     * The regex should be like "text(\\d+)other text" so the capture group is the next number.
     * @param toFilter The data to filter
     * @return The data without transmission and file guards.
     */
    @Override
    public List<String> filter(List<String> toFilter) {
        toFilter.forEach(this::filterLine);
        return toFilter;
    }

    /**
     * Each line can contain several events, separated by a newline
     * @param line The line to check
     */
    private void filterLine(String line) {
        // The input can be many lines in one element.
        for (String s : line.split("\n")) {
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                String numberString = matcher.group(1);
                long number = Long.parseLong(numberString);
                detector.check(number);
            }
        }
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "GapDetectionFilter" + System.lineSeparator() +
                regex + System.lineSeparator();
    }


    /**
     * Shutdown hook. print the result
     */
    @Override
    public void shutdown() {
        if (jsonReport) {
            System.out.println(detector.toJson());
        } else {
            System.out.println(detector);
        }
    }

    /**
     * Get the detector for this filter
     * @return the GapDetector
     */
    public GapDetector getDetector() {
        return detector;
    }

    /**
     * toString in json format
     * @return the internal state
     */
    public String toJson() {
        return detector.toJson();
    }

}
