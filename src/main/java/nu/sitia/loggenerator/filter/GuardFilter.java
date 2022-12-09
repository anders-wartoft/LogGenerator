package nu.sitia.loggenerator.filter;

import nu.sitia.loggenerator.util.Configuration;

import java.util.ArrayList;
import java.util.List;

public class GuardFilter implements ProcessFilter {

    private static final String GUARD_START = "--------";
    /**
     * Create a guardFilter and set all parameters
     * @param ignoredConfig The configuration object to get parameters from
     */
    public GuardFilter(Configuration ignoredConfig) {
    }

    /**
     * Don't add the line if it begins with --------
     * We think it's a guard in that case.
     * @param toFilter The data to filter
     * @return The data without transmission and file guards.
     */
    @Override
    public List<String> filter(List<String> toFilter) {
        List<String> filtered = new ArrayList<>();
        toFilter.forEach(s -> {
            if (!s.startsWith(GUARD_START)) {
                filtered.add(s);
            }
        });

        return filtered;
    }
}
