package nu.sitia.loggenerator.filter;

import nu.sitia.loggenerator.util.Configuration;

import java.util.*;

public class HeaderFilter implements ProcessFilter {

    private String header;
    /**
     * Create a HeaderFilter and set all parameters
     * @param config The configuration object to get parameters from
     */
    public HeaderFilter(Configuration config) {
        header = config.getHeader();
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
        String filteredHeader = Substitution.substitute(header, new HashMap<>(), new Date());
        return filteredHeader + toFilter;
    }

    @Override
    public List<String> filter(List<String> toFilter) {
        List<String> filtered = new ArrayList<>();
        toFilter.forEach(s ->
                filtered.add(filter(s)));

        return filtered;
    }
}
