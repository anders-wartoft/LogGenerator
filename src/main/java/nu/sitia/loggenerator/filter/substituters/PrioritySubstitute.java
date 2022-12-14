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
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
