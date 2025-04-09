/*
 * Copyright 2022 sitia.nu https://github.com/anders-wartoft/LogGenerator
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nu.sitia.loggenerator.inputitems;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class StringInputItemTestApp
    extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
public StringInputItemTestApp(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( StringInputItemTestApp.class );
    }

    
    /**
     * Test the StringInputItem
     */
    public void testStringInputItemOnce() {
        StringInputItem input = new StringInputItem(null);
        input.setParameter("--template", "once");
        input.setParameter("--from", "test");
        input.afterPropertiesSet();
        assertTrue(input.hasNext());
        List<String> lines = input.next();
        // print the result:
        System.out.println("Lines: " + lines);
        assertTrue(lines != null);
        if (lines != null) {
            assertTrue(lines.size() == 1);
            assertTrue(lines.get(0).equals("test"));    
        }
    }

    /**
     * Test the StringInputItem
     */
    public void testStringInputItemContinuous() {
        StringInputItem input = new StringInputItem(null);
        int nr = 10;
        input.setParameter("--template", "continuous");
        input.setParameter("--from", "test");
        input.setParameter("-l", String.valueOf(nr));
        input.afterPropertiesSet();
        assertTrue(input.hasNext());
        while(input.hasNext() && nr-- > 0) {
            List<String> lines = input.next();
            // print the result:
            System.out.println("Lines: " + lines);
            assertTrue(lines != null);
            if (lines != null) {
                assertTrue(lines.size() == 1);
                assertTrue(lines.get(0).equals("test"));    
            }
        }
        assertTrue(nr < 0);
        assertTrue(input.hasNext());
        input.teardown();
    }

    /**
     * Test the StringInputItem
     */
    public void testStringInputItemBatch() {
        StringInputItem input = new StringInputItem(null);
        input.setParameter("--template", "once");
        input.setParameter("--from", "test");
        input.setParameter("-bs", "100");
        input.afterPropertiesSet();
        assertTrue(input.hasNext());
        List<String> lines = input.next();
        // print the result:
        System.out.println("Lines: " + lines);
        assertTrue(lines != null);
        if (lines != null) {
            assertTrue(lines.size() == 100);
            assertTrue(lines.get(0).equals("test"));    
        }
    }

    /**
     * Test the StringInputItem
     */
    public void testStringInputItemTime() {
        int nr = 5; // We should get at least 5 events on 100ms
        StringInputItem input = new StringInputItem(null);
        input.setParameter("--template", "time:100"); // 100ms
        input.setParameter("--from", "test");
        input.afterPropertiesSet();
        assertTrue(input.hasNext());
        while(input.hasNext() && nr-- > 0) {
            List<String> lines = input.next();
            assertTrue(lines != null);
            if (lines != null) {
                assertTrue(lines.size() == 1);
                assertTrue(lines.get(0).equals("test"));    
            }
        }
    }


    /**
     * Test the StringInputItem
     */
    public void testStringInputItemVariable() {
        StringInputItem input = new StringInputItem(null);
        input.setParameter("--template", "once");
        input.setParameter("--from", "{oneOf:A,A,A,A}");
        input.afterPropertiesSet();
        assertTrue(input.hasNext());
        List<String> lines = input.next();
        // print the result:
        System.out.println("Lines: " + lines);
        assertTrue(lines != null);
        if (lines != null) {
            assertTrue(lines.size() == 1);
            assertTrue(lines.get(0).equals("A"));    
        }
    }
}
