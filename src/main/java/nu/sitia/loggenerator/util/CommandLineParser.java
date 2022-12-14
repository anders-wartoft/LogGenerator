package nu.sitia.loggenerator.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommandLineParser {

    private static final Map<String, String> seenParameters = new LinkedHashMap<>();

    /**
     * Helper function to get a command line argument
     * @param args The command line arguments
     * @param shortName A name abbreviation for the variable
     * @param longName The name of the variable
     * @param description A description for the variable
     * @return The value for that variable
     */
    public static String getCommandLineArgument(String [] args, String shortName, String longName, String description) {
        seenParameters.put(longName, description);
        String result = null;
        for (int i=0; i<args.length; i++) {
            String arg = args[i];
            String name1 = "-" + shortName;
            String name2 = "--" + longName;
            if (arg.equalsIgnoreCase(name1) || arg.equalsIgnoreCase(name2)) {
                if (i < args.length-1) {
                    result = args[i+1];
                } else {
                    System.err.println("-" + shortName + " or " + "--" + longName + " missing parameter. " + description);
                }
            }
        }
        return result;
    }

    /**
     * Return a list of parameters that can be set for this invocation
     * @return The parameters seen by this instance
     */
    public static Map<String, String> getSeenParameters() {
        return Collections.unmodifiableMap(seenParameters);
    }
}
