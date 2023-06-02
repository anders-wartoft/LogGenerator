package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


/**
 * Unit test for simple App.
 */
public class OneFromFileSubstituteTest
    extends TestCase {

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( OneFromFileSubstituteTest.class );
    }

    /**
     * Test oneFromFile
     */
    public void testOneFromFile() {
        String template = "test foo {oneFromFile:src/test/data/test.txt}";
        String actual = new OneFromFileSubstitute().substitute(template);
        assertEquals(19, actual.length());
    }
    /**
     * Test oneFromFile with encoding
     */
    public void testOneFromFileWithEncoding() {
        String fileName = "target/iso-8859-1.txt";
        String line = "ÅÄÖ";
        File file = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.ISO_8859_1);
             BufferedWriter writer = new BufferedWriter(osw)) {
            writer.append(line);
            writer.close();
            String expected = "test foo " + line;
            String template = "test foo {oneFromFile:" + fileName + "#ISO8859-1}";
            String actual = new OneFromFileSubstitute().substitute(template);
            assertEquals(expected, actual);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            file.delete();
        }


    }

}
