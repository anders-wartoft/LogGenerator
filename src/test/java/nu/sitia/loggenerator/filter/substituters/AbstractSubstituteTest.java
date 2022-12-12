package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Unit test for getExpressionEnd.
 * Abstract classes can't be instantiated so use a class
 * that extends the abstract class instead.
 */
public class AbstractSubstituteTest
    extends TestCase
{

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AbstractSubstituteTest.class );
    }


    /**
     * Test to extract an expression from a String
     */
    public void testMatchingBrace() {
        String template = "test {first:a,b,{second:d,e}}";
        int pos = template.indexOf("{first:");
        int end = PrioritySubstitute.getExpressionEnd(template, pos);
        assertEquals(29, end);
    }

    /**
     * Test to extract an expression from a String
     */
    public void testMatchingBrace2() {
        String template = "<{pri:}>{date:MMM DD HH:mm:ss} {oneOf:my-machine,your-machine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{random:1-65535}]: ";
        int pos = template.indexOf("{oneOf:");
        int end = PrioritySubstitute.getExpressionEnd(template, pos);
        assertEquals(94, end);
    }


}
