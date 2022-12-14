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
