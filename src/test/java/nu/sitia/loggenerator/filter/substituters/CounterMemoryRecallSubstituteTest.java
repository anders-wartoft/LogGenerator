package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class CounterMemoryRecallSubstituteTest
    extends TestCase {

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( CounterMemoryRecallSubstituteTest.class );
    }



    /**
     * Test counter generation
     */
    public void testCounterMemoryRecall() {
        String template = "{counter:myCounter:40}{cmr:invalidCounter}{cmr:myCounter}";
        String expected = "4040";
        // make sure to clear the counter after creation since the
        // internal state is static and the order of test cases matter
        CounterSubstitute substitute = new CounterSubstitute();
        substitute.clear();
        String actual = substitute.substitute(template);
        CounterMemoryRecallSubstitute cmr = new CounterMemoryRecallSubstitute();
        actual = cmr.substitute(actual);
        actual = cmr.substitute(actual);
        assertEquals(expected, actual);
        expected = "4141";
        CounterSubstitute cs = new CounterSubstitute();
        actual = cs.substitute(template);
        actual = cmr.substitute(actual);
        actual = cmr.substitute(actual);
        assertEquals(expected, actual);
    }

    /**
     * Test default name
     */
    public void testDefaultName() {
        String template = "{counter:40}{cmr}";
        String expected = "4040";
        // make sure to clear the counter after creation since the
        // internal state is static and the order of test cases matter
        CounterSubstitute substitute = new CounterSubstitute();
        substitute.clear();
        String actual = substitute.substitute(template);
        CounterMemoryRecallSubstitute cmr = new CounterMemoryRecallSubstitute();
        actual = cmr.substitute(actual);
        assertEquals(expected, actual);
        expected = "4141";
        CounterSubstitute cs = new CounterSubstitute();
        actual = cs.substitute(template);
        actual = cmr.substitute(actual);
        assertEquals(expected, actual);
    }


    /**
     * Test two different counters
     */
    public void testDifferentCounters() {
        String template = "{counter:40} {counter:other:1} {cmr} {cmr:other}";
        String expected = "40 1 40 1";
        // make sure to clear the counter after creation since the
        // internal state is static and the order of test cases matter
        CounterSubstitute subst = new CounterSubstitute();
        // Make sure previous tests are not affecting this test
        subst.clear();
        // Now, run the tests
        String firstInvocation = subst.substitute(template);
        String actual = subst.substitute(firstInvocation);
        CounterMemoryRecallSubstitute cmr = new CounterMemoryRecallSubstitute();
        actual = cmr.substitute(actual);
        actual = cmr.substitute(actual);
        assertEquals(expected, actual);

        String thirdInvocation = subst.substitute(template);
        actual = subst.substitute(thirdInvocation);
        actual = cmr.substitute(actual);
        actual = cmr.substitute(actual);
        expected = "41 2 41 2";
        assertEquals(expected, actual);
    }
}
