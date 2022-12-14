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

package nu.sitia.loggenerator;

import java.util.ArrayList;
import java.util.List;

public class Configuration {
        /** Guard sent as an event for each transmission if statistics is enabled */
        public static final String BEGIN_TRANSACTION_TEXT = "--------BEGIN_TRANSACTION--------";
        /** Guard sent as an event for each file if statistics is enabled */
        public static final String BEGIN_FILE_TEXT = "--------BEGIN_FILE--------";

        /** Guard sent as an end event for each transmission if statistics is enabled */
        public static final String END_TRANSACTION_TEXT = "--------END_TRANSACTION--------";
        /** Guard sent as an end event for each file if statistics is enabled */
        public static final String END_FILE_TEXT = "--------END_FILE--------";
        /** Start of transaction */
        public static final List<String> BEGIN_TRANSACTION = new ArrayList<>();
        /** Start of file */
        public static final List<String> BEGIN_FILE = new ArrayList<>();
        /** End of transaction */
        public static final List<String> END_TRANSACTION = new ArrayList<>();
        /** End of file */
        public static final List<String> END_FILE = new ArrayList<>();

        static {
            BEGIN_TRANSACTION.add(BEGIN_TRANSACTION_TEXT);
            BEGIN_FILE.add(BEGIN_FILE_TEXT);
            END_TRANSACTION.add(END_TRANSACTION_TEXT);
            END_FILE.add(END_FILE_TEXT);
        }
}
