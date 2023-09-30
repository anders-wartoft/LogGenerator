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

    /**
     * Custom variables
     */
    public void testIpv6()
    {
        String [] args = {
        };
        List<String> data = Arrays.asList("{ipv6}");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String today = sdf.format(new Date());

        Configuration config = new Configuration(args);
        SubstitutionFilter filter = new SubstitutionFilter(config);
        List<String> result = filter.filter(data);

        assertTrue(result.get(0).length() == 39);
    }


    /**
     * Test jsonfilter. Let the input be a json structure,
     * get a part of the json structure and do a
     * gap detection on that part.
     */
    public void testJson() {
        String [] input = {
                "{\"_index\":\"testindex\",\"_score\":1.0,\"_source\":{\"_id\":\"test-1\",\"@timestamp\":\"2023-09-29T20:24:29\",\"message\":\"Test row 11\"}}",
                "{\"_index\":\"testindex\",\"_score\":1.0,\"_source\":{\"_id\":\"test-1\",\"@timestamp\":\"2023-09-29T20:24:29\",\"message\":\"Test row 11\"}}",
                "{\"_index\":\"testindex\",\"_score\":1.0,\"_source\":{\"_id\":\"test-2\",\"@timestamp\":\"2023-09-29T20:24:29\",\"message\":\"Test row 22\"}}",
                "{\"_index\":\"testindex\",\"_score\":1.0,\"_source\":{\"_id\":\"test-11\",\"@timestamp\":\"2023-09-29T20:24:29\",\"message\":\"Test row 33\"}}",
                "{\"_index\":\"testindex\",\"_score\":1.0,\"_source\":{\"_id\":\"test-11\",\"@timestamp\":\"2023-09-29T20:24:29\",\"message\":\"Test row 33\"}}",
                "{\"_index\":\"testindex\",\"_score\":1.0,\"_source\":{\"_id\":\"test-22\",\"@timestamp\":\"2023-09-29T20:24:29\",\"message\":\"Test row 44\"}}"
        };
        String path = "_source->_id";

        JsonFilter jsonFilter = new JsonFilter(path);
        String regex = "test-(\\d+)$";
        GapDetectionFilter gapDetector = new GapDetectionFilter(regex, true);
        List<String> result1 = jsonFilter.filter(List.of(input));
        List<String> result2 = gapDetector.filter(result1);
        String expected = "{\"gaps\":[{\"from\":3,\"to\":10},{\"from\":12,\"to\":21}],\"unique\":4,\"duplicates\":[{\"1\":2},{\"11\":2}],\"next\":23}";
        assertEquals(expected, gapDetector.toJson());

    }

}