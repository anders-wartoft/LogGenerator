package nu.sitia.loggenerator.filter.substituters;

import java.util.*;


public class Substitution {

    /**
     * The substitutes to iterate over
     */
    private final List<Substitute> items = new LinkedList<>();

    /**
     * Cache the date object to update the date() for each invocation
     */
    private final DateSubstitute dateSubstitute;

    /**
     * Create the list of substitutes
     */
    public Substitution() {
        dateSubstitute = new DateSubstitute();
        items.add(dateSubstitute);
        items.add(new CounterSubstitute());
        items.add(new Ipv4Substitute());
        items.add(new LoremSubstitute());
        items.add(new OneOfSubstitute());
        items.add(new PrioritySubstitute());
        items.add(new RandomNumberSubstitute());
        items.add(new StringSubstitute());
    }

    /**
     * Freemarker-like substitution.
     * All occurrences of {key} in the template will be replaced with value from the
     * translations map.
     *
     * @param template     The template containing {key} values
     * @param translations The key-value pairs
     * @return The template with all {key} substituted with value
     */
    public String substitute(String template, Map<String, String> translations, Date date) {
        // Update the timestamp
        dateSubstitute.setDate(date);

        String result = template;
        // Start with built-in values, like {syslog-header}, {ip}:
        for (String key : translations.keySet()) {
            String value = translations.get(key);
            result = result.replaceAll("\\{" + key + "}", value);
        }

        // Now, do the list of {date: ...
        String lastResult;
        do {
            lastResult = result;
            for (Substitute item : items) {
                result = item.substitute(result);
            }
        } while (!lastResult.equals(result));
        return result;
    }
}
