package nu.sitia.loggenerator.templates;

import java.util.Date;

public class TemplateFactory {

    /**
     * Get a template object from a string
     * @param template The string
     * @return A Template object
     */
    public static Template getTemplate(String template) {
        if (template != null) {
            if ("continuous".equalsIgnoreCase(template)) {
                return (new ContinuousTemplate());
            } else if ("file".equalsIgnoreCase(template)) {
                return new FileTemplate();
            } else if ("none".equalsIgnoreCase(template)) {
                return new NoneTemplate();
            } else if (template.startsWith("time:")) {
                String [] parts = template.split(":");
                if (parts.length != 2) {
                    throw new RuntimeException("Illegal template time value. Set as time:30 for 30000 seconds. Value was: " + template);
                }
                // Set the time to end time
                return new TimeTemplate (Long.parseLong(parts[1]) + new Date().getTime());

            } else {
                throw new RuntimeException("Illegal template value. Legal values are: continuous, file, time or none. Value was: " + template);
            }
        } else { // null
            return new NoneTemplate();
        }
    }
}
