/*
 * Copyright 2022 sitia.nu https://github.com/anders-wartoft/LogGenerator
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
