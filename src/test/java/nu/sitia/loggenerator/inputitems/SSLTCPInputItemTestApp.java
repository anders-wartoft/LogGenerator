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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SSLTCPInputItemTestApp
    extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
public SSLTCPInputItemTestApp(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( SSLTCPInputItemTestApp.class );
    }

    /**
     * TCP SSL Test
     */
    public void testSend() {
//        String host = "localhost";
//        Integer port = 9999;
//        SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
//        SSLSocket sslsocket = null;
//        try {
//            sslsocket = (SSLSocket) sslsocketfactory
//                    .createSocket(host, port);
//            InputStream in = sslsocket.getInputStream();
//            OutputStream out = sslsocket.getOutputStream();
//
//            out.write("GET / HTTP/1.1\n".getBytes());
//            out.write(("Host: " + host + ":" + port + "\n").getBytes());
//            out.write(("User-Agent: Java application\n").getBytes());
//            out.write(("Accept: */*\n\n").getBytes());
//            out.flush();
//            while (in.available() > 0) {
//                System.out.print(in.read());
//            }
//
//            System.out.println("Secured connection performed successfully");
//
//        } catch (Exception e) {
//            fail(e.getMessage());
//        }
    }
}
