package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class CounterSubstituteTest
    extends TestCase {

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( CounterSubstituteTest.class );
    }



    /**
     * Test counter generation
     */
    public void testCounter() {
        String template = "{counter:myCounter:40}";
        String expected = "40";
        String actual = new CounterSubstitute().substitute(template);
        assertEquals(expected, actual);
        expected = "41";
        actual = new CounterSubstitute().substitute(template);
        assertEquals(expected, actual);
    }
    
}
