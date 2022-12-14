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

import nu.sitia.loggenerator.util.Ipv4;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ipv4Substitute extends AbstractSubstitute {

    /** Ipv4 variable regex */
    private static final String ipv4Regex = "\\{ipv4:(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})/(\\d{1,2})}";

    /** Cached pattern for ipv4 */
    private static final Pattern ipv4Pattern = Pattern.compile(ipv4Regex);


    /**
     * An IP variable is in the form:
     * {ipv4:192.168.1.1/24}
     * A random ip in that range will be inserted instead of the variable specification.
     * @param input The string containing the ip variable specification
     * @return The input but with an actual ip number instead of the specification
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{ipv4:");
        if (startPos < 0) return input;

        int endPos = AbstractSubstitute.getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the ip subnet and mask string
        Matcher matcher = ipv4Pattern.matcher(part);
        if (matcher.find()) {
            String ipString = matcher.group(1);
            String cidr = matcher.group(2);
            String result = Ipv4.longToIpv4(Ipv4.getRandomIpv4(ipString, cidr));
            return input.substring(0, startPos) + result + input.substring(endPos);
        }
        throw new RuntimeException(("Illegal ipv4 pattern: " + input));
    }
}
