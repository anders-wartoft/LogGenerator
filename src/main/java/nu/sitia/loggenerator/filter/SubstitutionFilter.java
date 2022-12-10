package nu.sitia.loggenerator.filter;

import nu.sitia.loggenerator.util.Configuration;

import java.util.*;

public class SubstitutionFilter implements ProcessFilter {

    /** Other variables we want to change. Name, Value */
    private final Map<String, String> variableMap;
    /**
     * Create a filter and set all parameters
     * @param config The configuration object to get parameters from
     */
    public SubstitutionFilter(Configuration config) {
        variableMap = config.getVariableSubstitutions();
    }

    /**
     * Change all variables to values
     * @param toFilter The data to filter
     * @return The value of the variable
     */
    @Override
    public List<String> filter(List<String> toFilter) {
        List<String> filtered = new ArrayList<>();
        toFilter.forEach(s -> filtered.add(Substitution.substitute(s, variableMap, new Date())));
        return filtered;
    }
}
