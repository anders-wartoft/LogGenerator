package nu.sitia.loggenerator.filter;


import nu.sitia.loggenerator.Configuration;

import java.util.LinkedList;
import java.util.List;

public class GuardFilter implements ProcessFilter {

    /**
     * Create a guardFilter and set all parameters
     * @param ignoredConfig The configuration object to get parameters from
     */
    public GuardFilter(String [] ignoredConfig) {
    }

    /**
     * Don't add the line if it is a statistics guard
     * @param toFilter The data to filter
     * @return The data without transmission and file guards.
     */
    @Override
    public List<String> filter(List<String> toFilter) {
        List<String> result = new LinkedList<>();
        toFilter.forEach(s -> result.add(filterLine(s)));

        return result;
    }

    /**
     * Each line can contain several events, separated by a newline
     * @param line The line to check
     */
    private String filterLine(String line) {
        List<String> result = new LinkedList<>();

        // The input can be many lines in one element.
        for (String s : line.split("\n")) {
            if (!removeLine(s)) {
                result.add(s);
            }
        }
        return String.join("\n", result);
    }


    /**
     * If the line contains any guard, then return true
     * @param toCheck The line to check
     * @return True iff toCheck contains a guard
     */
    public boolean removeLine(String toCheck) {
        return (toCheck.startsWith(Configuration.BEGIN_TRANSACTION_TEXT)
            ||  toCheck.startsWith(Configuration.END_TRANSACTION_TEXT)
            ||  toCheck.startsWith(Configuration.BEGIN_FILE_TEXT)
            ||  toCheck.startsWith(Configuration.END_FILE_TEXT));
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "GuardFilter" + System.lineSeparator();
    }
}
