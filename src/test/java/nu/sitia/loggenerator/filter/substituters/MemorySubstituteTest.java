package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Date;
import java.util.HashMap;

/**
 * Unit test for simple App.
 */
public class MemorySubstituteTest
    extends TestCase {

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(MemorySubstituteTest.class);
    }


    /**
     * Test memory generation
     */
    public void testMemorySet() {
        String template = "{ms/40}";
        String expected = "40";
        // make sure to clear the counter after creation since the
        // internal state is static and the order of test cases matter
        MemorySetSubstitute substitute = new MemorySetSubstitute();
        substitute.clear();
        String actual = substitute.substitute(template);
        assertEquals(expected, actual);
        assertEquals("40", MemorySetSubstitute.getCounterValue(null));
    }

    /**
     * Test non-default name
     */
    public void testNonDefaultName() {
        String template = "{ms:name/40}";
        String expected = "40";
        MemorySetSubstitute substitute = new MemorySetSubstitute();
        substitute.clear();
        String actual = substitute.substitute(template);
        assertEquals(expected, actual);
        assertEquals("40", MemorySetSubstitute.getCounterValue("name"));
    }


    /**
     * Test two different memories
     */
    public void testDifferentCounters() {
        String template = "{ms/40} {ms:other/1}";
        String expected = "40 1";
        MemorySetSubstitute subst = new MemorySetSubstitute();
        subst.clear();
        String firstInvocation = subst.substitute(template);
        String actual = subst.substitute(firstInvocation);
        assertEquals(expected, actual);
        assertEquals("40", MemorySetSubstitute.getCounterValue(null));
        assertEquals("1", MemorySetSubstitute.getCounterValue("other"));
    }


    /**
     * Test memory generation
     */
    public void testMemoryRecall() {
        String template = "{ms/40} {mr}";
        String expected = "40 40";

        Substitution substitute = new Substitution();
        String actual = substitute.substitute(template, new HashMap<>(), new Date());
        assertEquals(expected, actual);
    }

    /**
     * Test non-default name
     */
    public void testRecallNonDefaultName() {
        String template = "{ms:name/40} {mr:name}";
        String expected = "40 40";

        Substitution substitute = new Substitution();
        String actual = substitute.substitute(template, new HashMap<>(), new Date());
        assertEquals(expected, actual);
    }


    /**
     * Test two different memories
     */
    public void testRecallDifferentCounters() {
        String template = "{ms/40} {ms:other/1} {mr} {mr:other}";
        String expected = "40 1 40 1";

        Substitution substitute = new Substitution();
        String actual = substitute.substitute(template, new HashMap<>(), new Date());
        assertEquals(expected, actual);
    }

    /**
     * Test un-initialized name
     */
    public void testRecallMissing() {
        String template = "{ms:name/40} {mr:namemissing}";
        String expected = "40 ";

        Substitution substitute = new Substitution();
        String actual = substitute.substitute(template, new HashMap<>(), new Date());
        assertEquals(expected, actual);
    }

    /**
     * Test un-initialized name
     */
    public void testWithRecursion() {
        String template = "{ms:other/{oneOf:a,b,c,d}} {mr:other}";
        String expected = "40 ";

        Substitution substitute = new Substitution();
        String actual = substitute.substitute(template, new HashMap<>(), new Date());

        boolean ok = false;
        if ("a a".equals(actual)
                || "b b".equals(actual)
                || "c c".equals(actual)
                || "d d".equals(actual)) {
            ok = true;
        }
        assertTrue(ok);
    }

    /**
     * Test un-initialized name
     */
    public void testWithRecursion10000() {
        for (int i = 0; i < 10000; i++) {
            testWithRecursion();
        }
    }
}