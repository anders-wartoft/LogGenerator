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

package nu.sitia.loggenerator.filter;

import junit.framework.TestCase;
import nu.sitia.loggenerator.Configuration;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SubstitutionFilterTest extends TestCase {

    /**
     * Custom variables
     */
    public void testCustomVariables()
    {
        String [] args = {
                "-p", "src/test/data/config.properties"
        };

        Configuration config = new Configuration(args);
        SubstitutionFilter filter = new SubstitutionFilter(config);
        assertEquals("Sitia.nu", filter.getVariableMap().get("my-name"));

        assertEquals("Sitia.nu", filter.getVariableMap().get("my-name"));
        assertEquals("{ip}", filter.getVariableMap().get("new-variable"));
    }

    /**
     * Custom variables
     */
    public void testStandardVariables()
    {
        String [] args = {
                "-p", "src/test/data/config.properties"
        };

        Configuration config = new Configuration(args);
        SubstitutionFilter filter = new SubstitutionFilter(config);

        assertEquals("{<ipv4:0.0.0.0/0}", filter.getVariableMap().get("ip"));
    }

    /**
     * Custom variables
     */
    public void testOverwriteStandardVariables()
    {
        String [] args = {
                "-p", "src/test/data/overwrite.properties"
        };

        Configuration config = new Configuration(args);
        SubstitutionFilter filter = new SubstitutionFilter(config);

        assertEquals("192.168.1.1", filter.getVariableMap().get("ip"));
    }

    /**
     * Custom variables
     */
    public void testTimeOffset()
    {
        long msDay = -1000 * 3600 * 24;
        String [] args = {
                "-to", String.valueOf(msDay) // yesterday
        };
        List<String> data = Arrays.asList("{date:yyyyMMdd}");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String today = sdf.format(new Date());

        Configuration config = new Configuration(args);
        SubstitutionFilter filter = new SubstitutionFilter(config);
        List<String> result = filter.filter(data);

        assertFalse(today.equals(result.get(0)));
    }

}