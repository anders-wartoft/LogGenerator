package nu.sitia.loggenerator.filter;

import java.util.List;

public interface ProcessFilter {

    /**
     * The alteration function implemented as a set of filters
     * @param toFilter The data to filter
     * @return Filtered (altered) data
     */
     List<String> filter (List<String> toFilter);

    /**
     * Return the configuration in printable format
     * @return The configuration, ending with newline
     */
    String toString();
}
