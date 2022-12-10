package nu.sitia.loggenerator.filter;

import nu.sitia.loggenerator.util.Configuration;

public class FilterFactory {
    /**
     * Create a filter specified by a name.
     * Filter properties may be given in the configuration object
     * @param name The name of the filter class
     * @param config The configuration object
     * @return An instantiated filter
     */
    public static ProcessFilter createFilter(String name, Configuration config) {
        return switch (name) {
            case "header", "HEADER" -> new HeaderFilter(config);
            case "regex", "REGEX" -> new RegexFilter(config);
            case "substitution", "SUBSTITUTION" -> new SubstitutionFilter(config);
            default -> throw new RuntimeException("Illegal filter type: " + name);
        };

    }
}
