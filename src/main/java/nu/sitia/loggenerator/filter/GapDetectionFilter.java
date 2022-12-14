package nu.sitia.loggenerator.filter;

import nu.sitia.loggenerator.ShutdownHandler;
import nu.sitia.loggenerator.util.gapdetector.GapDetector;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GapDetectionFilter implements ProcessFilter, ShutdownHandler {
    /** The detector */
    private final GapDetector detector = new GapDetector();

    /** The pattern to get the next number from the log */
    private final Pattern pattern;

    /** Used in toString() */
    private final String regex;

    /**
     * Create a guardFilter and set all parameters
     * @param regex The regex to use to identify the id number
     */
    public GapDetectionFilter(String regex) {
        if (null == regex) {
            throw new RuntimeException("No gap regex detected");
        }
        pattern = Pattern.compile(regex);
        this.regex = regex;
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
        System.out.println(detector);
    }
}
