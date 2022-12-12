package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Unit test for simple App.
 */
public class DateSubstituteTest
    extends TestCase
{


    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( DateSubstituteTest.class );
    }




    /**
     * Test date patterns
     */
    public void testDatePatterns() {
        String template = "{date:yyyy-MM-dd}";
        String expected = "2100-01-01";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date;
        try {
            date = formatter.parse(expected);
            DateSubstitute substitute = new DateSubstitute();
            substitute.setDate(date);
            String actual = substitute.substitute(template);
            assertEquals(expected, actual);
        } catch (ParseException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test date patterns
     */
    public void testDatePatternsWithLocale() {
        String template = "{date:yyyy-MM-dd/sv:SE}";
        String expected = "2100-01-01";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date;
        try {
            date = formatter.parse(expected);
            DateSubstitute substitute = new DateSubstitute();
            substitute.setDate(date);
            String actual = substitute.substitute(template);
            assertEquals(expected, actual);
        } catch (ParseException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test date patterns
     */
    public void testDatePatternsWithLocale2() {
        String template = "{date:MMM dd HH:mm:ss/en:US}";
        String dateString = "2100-01-01";
        String expected = "Jan 01 00:00:00";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date;
        try {
            date = formatter.parse(dateString);
            DateSubstitute substitute = new DateSubstitute();
            substitute.setDate(date);
            String actual = substitute.substitute(template);
            assertEquals(expected, actual);
        } catch (ParseException e) {
            fail(e.getMessage());
        }
    }


}
