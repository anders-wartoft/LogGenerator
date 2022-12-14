package nu.sitia.loggenerator.filter;

import nu.sitia.loggenerator.filter.substituters.Substitution;

import java.util.*;

public class HeaderFilter implements ProcessFilter {
    /** Cached header template */
    private final String header;

    /** Cached list of substitute */
    private final Substitution substitution = new Substitution();

    /**
     * Create a HeaderFilter and set all parameters
     * @param header The header to prepend every event with
     */
    public HeaderFilter(String header) {
        this.header = header;
        if (null == header) {
            throw new RuntimeException("Header is null");
        }
    }

    /**
     * Filter one string
     *
     * @param toFilter The string to change
     * @return toFilter with a header added before the string.
     */
    private String filter(String toFilter) {
        // Fix possible {date...} fields, and {lorem, ...:
        String filteredHeader = substitution.substitute(header, new HashMap<>(), new Date());
        return filteredHeader + toFilter;
    }

    @Override
    public List<String> filter(List<String> toFilter) {
        List<String> filtered = new ArrayList<>();
        toFilter.forEach(s ->
                filtered.add(filter(s)));

        return filtered;
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "HeaderFilter" + System.lineSeparator() + header + System.lineSeparator();
    }
}
