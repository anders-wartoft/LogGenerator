package nu.sitia.loggenerator.filter.substituters;

import java.util.Random;

public class PrioritySubstitute extends AbstractSubstitute {

    /**
     * A valid syslog priority.
     * @param input The string containing the pri variable specification
     * @return The input but with an actual priority number instead of the specification
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{pri:}");
        if (startPos < 0) return input;

        int endPos = startPos + "{pri:}".length();

        Random random = new Random();
        // Facility
        // 0 -- 23
        int facility = random.nextInt(24);
        // Severity
        // 0 -- 7
        int severity = random.nextInt(8);
        int priority = 8 * facility + severity;
        return input.substring(0, startPos) + priority + input.substring(endPos);
    }
}
