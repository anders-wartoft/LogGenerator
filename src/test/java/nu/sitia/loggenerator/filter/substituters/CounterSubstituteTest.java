package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashMap;

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

    /**
     * Test default name
     */
    public void testDefaultName() {
        String template = "{counter:40}";
        String expected = "40";
        String actual = new CounterSubstitute().substitute(template);
        assertEquals(expected, actual);
        expected = "41";
        actual = new CounterSubstitute().substitute(template);
        assertEquals(expected, actual);
    }


    /**
     * Test two different counters
     */
    public void testDifferentcounters() {
        String template = "{counter:40} {counter:other:1}";
        String expected = "40 1";
        CounterSubstitute subst = new CounterSubstitute();
        // Make sure previous tests are not affecting this test
        subst.clear();
        // Now, run the tests
        String firstInvocation = subst.substitute(template);
        String actual = subst.substitute(firstInvocation);
        assertEquals(expected, actual);

        String thirdInvocation = subst.substitute(template);
        actual = subst.substitute(thirdInvocation);
        expected = "41 2";
        assertEquals(expected, actual);
    }
}
