package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Unit test for simple App.
 */
public class RepeatSubstituteTest
    extends TestCase {

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( RepeatSubstituteTest.class );
    }



    /**
     * Test repeat generation
     */
    public void testBasic() {
        String template = "{repeat:10#a#}";
        String expected = "aaaaaaaaaa";
        String actual = new RepeatSubstitute(new Substitution()).substitute(template);
        assertEquals(expected, actual);
    }


    /**
     * Test repeat generation
     */
    public void testRecursive() {
        String template = "{repeat:{random:8-8}#a#}";
        String actual = new RepeatSubstitute(new Substitution()).substitute(template);
        assertEquals("aaaaaaaa", actual);
    }

    /**
     * Test repeat generation
     */
    public void testDelimiter() {
        String template = "{repeat:{random:8-8}#a#-#}";
        String actual = new RepeatSubstitute(new Substitution()).substitute(template);
        assertEquals("a-a-a-a-a-a-a-a", actual);
    }

}
