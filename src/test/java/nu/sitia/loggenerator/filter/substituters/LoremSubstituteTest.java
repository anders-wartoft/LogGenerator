package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class LoremSubstituteTest
    extends TestCase {

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( LoremSubstituteTest.class );
    }


    /**
     * Test lorem
     */
    public void testLorem() {
        String template = "{lorem:1:word/ }";
        String expected = "word";
        String actual = new LoremSubstitute().substitute(template);
        assertEquals(expected, actual);
    }

    /**
     * Test lorem
     */
    public void testLorem2() {
        String template = "{lorem:word word2 word3/ }";
        try {
            new LoremSubstitute().substitute(template);
            fail("Should throw exception");
        } catch (RuntimeException e) {
            // ignore. Should throw exception
        }

    }

    /**
     * Test lorem
     */
    public void testLorem3() {
        String template = "{lorem:4:word/ }";
        String expected = "word word word word";
        String actual = new LoremSubstitute().substitute(template);
        assertEquals(expected, actual);
    }

    /**
     * Test lorem
     */
    public void testLorem4() {
        String template = "{lorem:42:create a story by scrambling a lot of words/ }";
        String actual = new LoremSubstitute().substitute(template);
        assertNotSame(template, actual);
    }
    
}
