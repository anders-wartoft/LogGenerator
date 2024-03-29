package nu.sitia.loggenerator.filter;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;


/**
 * Unit test for simple App.
 */
public class JsonTest
    extends TestCase
{

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public JsonTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( JsonTest.class );
    }

    /**
     * Test jsonfilter
     */
    public void testJson() {
        String input = "{\"query\": { \"query_string\": { \"query\": \"*\" }}, \"_source\": [\"_id\"]}";
        String path = "query.query_string.query";
        String expected = "*";

        JsonFilter jsonFilter = new JsonFilter(null);
        jsonFilter.setParameter("--path", path);
        jsonFilter.afterPropertiesSet();

        List<String> result = jsonFilter.filter(List.of(input));

        assertEquals(expected, result.get(0));
    }


    /**
     * Test jsonfilterArray
     */
    public void testJsonArray() {
        String input = "{\"cars\": { \"makes\": [\"Ford\", \"BMW\", \"Fiat\"] }}";
        String path = "cars.makes";

        JsonFilter jsonFilter = new JsonFilter(null);
        jsonFilter.setParameter("--path", path);
        jsonFilter.afterPropertiesSet();

        List<String> result = jsonFilter.filter(List.of(input));
        assertEquals(3, result.size());
    }

}
