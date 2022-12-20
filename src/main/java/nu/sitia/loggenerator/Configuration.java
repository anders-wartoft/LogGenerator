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
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nu.sitia.loggenerator;


import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class Configuration {
        static final Logger logger = Logger.getLogger(Configuration.class.getName());

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

        /** The class that holds the shorthand and long parameter keys as well as a description */
        private record Item(String shortName, String longName, String description) { }

        /** A list of valid parameter keys */
        static final List<Item> keys = new ArrayList<>();

        static {
                keys.add(new Item("-h", "--help", "Show the help information"));
                keys.add(new Item("-i", "--input", "The input module to use. Valid arguments are: counter, file, kafka, static, tcp, udp or template"));
                keys.add(new Item("-o", "--output", "The output module to use. Valid arguments are: cmd, file, kafka, null, tcp or udp"));
                keys.add(new Item("-ifn", "--input-file-name", "The file name to use for the input module. Can be a file or a directory."));
                keys.add(new Item("-g", "--glob", "If -fn denotes a directory, this glob can be used to select some of the files in the directory. The globs are java.nio globs. Remember to put asterisks (*) within quotes so that the OS doesn't expand the globs before LogGenerator can get them."));
                keys.add(new Item("-ofn", "--output-file-name", "The file name to use for the output module."));
                keys.add(new Item("-string", "--string", "The string to send if input is 'static' or 'counter'"));
                keys.add(new Item("-icn", "--input-client-name", "The client id for Kafka input modules"));
                keys.add(new Item("-itn", "--input-topic-name", "The name of the topic to read from"));
                keys.add(new Item("-ibs", "--input-bootstrap-server", "The Kafka hostname:port number to connect to for reading"));
                keys.add(new Item("-ocn", "--output-client-name", "The client id for Kafka output modules"));
                keys.add(new Item("-otn", "--output-topic-name", "The name of the topic to write to"));
                keys.add(new Item("-obs", "--output-bootstrap", "The Kafka hostname:port number to connect to for writing"));
                keys.add(new Item("-s", "--statistics", "Add guard messages to the sent data to be able to calculate eps etc."));
                keys.add(new Item("-ih", "--input-host", "The address to bind to if listening to UDP or TCP input. If not added, the socket will bind to all interfaces"));
                keys.add(new Item("-oh", "--output-host", "The address to send UDP or TCP data to. Also add -po port number"));
                keys.add(new Item("-ip", "--input-port", "The port to listen to"));
                keys.add(new Item("-op", "--output-port", "The port to send UDP or TCP data to. Also add -ho hostname"));
                keys.add(new Item("-l", "--limit", "Limit the amount of sent events to the argument number of events"));
                keys.add(new Item("-t", "--template", "Load a file as a template and resolve variables before sending. The file to send is added with -fn or --file-name Values can be one of:" + System.lineSeparator() +
                        "    'none' - send the file without expanding the variables but in a random order" + System.lineSeparator() +
                        "    'continuous' - send a random row from the file with variables expanded. The same row can be sent several times. If you just want a specified number of events, add the -l (--limit) parameter to stop sending after the specified number of events." + System.lineSeparator() +
                        "    'time:{number}' - as 'continuous' but end the transmission after {number} ms" + System.lineSeparator() +
                        "    'file' - send the file in random order with variables expanded"));
                keys.add(new Item("-ib", "--input-batch-size", "How many rows to read before sending to processing"));
                keys.add(new Item("-ob", "--output-batch-size", "How many rows to write (new line separated) every time the send method is called"));
                keys.add(new Item("-gd", "--gap-detection", "Regex to find the event serial number. In the regex, the first capture group must be the number, e.g. \"<(\\d+)>\""));
                keys.add(new Item("-rg", "--remove-guard", "Remove the statistics events (BEGIN_TRANSACTION, ...)"));
                keys.add(new Item("-he", "-header", "String to add to the beginning of each entry. May contain variables."));
                keys.add(new Item("-r", "--regex", "Regex to search for and replace with a value."));
                keys.add(new Item("-v", "--value", "Value to replace the regex with."));
                keys.add(new Item("-e", "--eps", "Max eps to send (events per second). Throttle if above."));
                keys.add(new Item("-pp", "--printouts", "Milliseconds between extra printout for statistics."));
                keys.add(new Item("-p", "--property-file", "Load configuration from a property file. The argument is the name of the file to load."));
                keys.add(new Item("-vf", "--variable-file", "Property file with variable definitions"));

        }
        static final Map<String, String> standardVariables = new HashMap<>();
        static {
                standardVariables.put("syslog-header", "<{pri:}>{date:MMM dd HH:mm:ss} {oneOf:my-machine,your-machine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{random:1-65535}]: ");
                standardVariables.put("ip", "{<ipv4:0.0.0.0/0}");
                standardVariables.put("rfc1918","{oneOf:{ipv4:192.168.0.0/16},{ipv4:172.16.0.0/12},{ipv4:10.0.0.0/8}}");
        }

        static final Map<String, String> customVariables = new HashMap<>();

        /**
         * Get all standard and custom variables
         * @return A map of name-value for all variable substitutions we'd like to make
         */
        public Map<String, String> getVariableMap() {
                Map<String, String> result = new HashMap<>();
                result.putAll(standardVariables);
                result.putAll(customVariables);
                return result;
        }

        /** The actual parameters read from command line, yaml or property file */
        private final Map<Item, String> parameters = new HashMap<>();

        /**
         * Get an item for the supplied name
         * @param key The input (e.g., -ho or --host-output)
         * @return The Item containing that key in shortName or longName
         */
        private Item getItemFromString(String key) {
                for (Item item: keys) {
                        if (item.shortName.equalsIgnoreCase(key)
                          || item.longName.equalsIgnoreCase(key)) {
                                return item;
                        }
                }
                return null;
        }

        /**
         * Get a configuration value
         * @param string A string like -h or --help
         * @return A value from the configuration for that key, or null if not found
         */
        public String getValue(String string) {
                Item item = getItemFromString(string);
                if (item != null) {
                        // may be null
                        return parameters.get(item);
                }
                // not found
                return null;
        }

        /**
         * Print help information
         */
        private void printHelp() {
                System.out.println("Arguments: ");
                for (Item key:keys) {
                        System.out.println(key.shortName + " " + key.longName + " " + key.description);
                }
        }

        /**
         * If a parameter is not found, get a message to the user
         * @param string The key not found
         * @return A missing argument string for display
         */
        public String getNotFoundInformation(String string) {
                Item item = getItemFromString(string);
                if (null == item) {
                        return "Bug detected. " + string + " should not be an argument.";
                }
                return "Missing argument (" + item.shortName + ") or (" + item.longName + "). " + item.description;
        }

        /**
         * Create a new configuration object and get the parameters from the user
         * @param args The command line arguments
         */
        public Configuration(String [] args) {
                for (int i=0; i<args.length; i+=2) {
                        String key = args[i];
                        Item item = getItemFromString(key);
                        if (item != null) {
                                if (item.longName.equalsIgnoreCase("--help")) {
                                        printHelp();
                                        System.exit(0);
                                }
                                if (parameters.containsKey(item)) {
                                        throw new RuntimeException("Argument " + item.shortName + " (" + item.longName + ") has already been declared. ");
                                }
                                parameters.put(item, args[i+1]);
                        } else {
                                printHelp();
                                throw new RuntimeException("Unrecognized argument: " + key);
                        }
                }
                if (getValue("--property-file") != null) {
                        String fileName = getValue("--property-file");
                        // Read the configuration from the property file
                        readConfigFromPropertyFile(fileName);

                }
                if (getValue("--variable-file") != null) {
                        String fileName = getValue("--variable-file");
                        readVariablesFromPropertyFile(fileName);
                }
        }

        /**
         * Load configuration from a file
         * @param fileName The file to use
         */
        protected void readConfigFromPropertyFile(String fileName) {
                File file = new File(fileName);
                if (!file.exists()) {
                        throw new RuntimeException("The property file " + file.getAbsolutePath() + " can't be found.");
                }
                logger.config("Reading configuration from : " + file.getAbsolutePath());
                try {
                        FileInputStream input = new FileInputStream(fileName);
                        Scanner scanner = new Scanner(input);
                        int lineNumber = 1;
                        while (scanner.hasNextLine()) {
                                String line = scanner.nextLine();
                                if (!line.startsWith("#")) {
                                        int index = line.indexOf("=");
                                        if (index < 1) {
                                                logger.fine("Disregarding line " + lineNumber + " due to missing = character");
                                        } else {
                                                String key = line.substring(0, index);
                                                String value = line.substring(index + 1);
                                                if (key.startsWith("custom.")) {
                                                        customVariables.put(key.substring("custom.".length()), value);
                                                } else {
                                                        Item item = getItemFromString("-" + key);
                                                        if (null == item) {
                                                                item = getItemFromString("--" + key);
                                                        }
                                                        if (null == item) {
                                                                throw new RuntimeException("Unknown key: " + key + " on line " + lineNumber + " in property file: " + file.getAbsolutePath());
                                                        }
                                                        logger.finer(item.longName + ": " + value);
                                                        parameters.put(item, value);
                                                }
                                        }
                                }
                                lineNumber++;
                        }
                } catch (IOException e) {
                        throw new RuntimeException("Exception trying to load file: " + file.getAbsolutePath(), e);
                }
        }

        /**
         * Should statistics messages and outputs be used?
         * @return true Iff statistics is enabled
         */
        public boolean isStatistics() {
                return "true".equalsIgnoreCase(getValue("-s"));
        }

        /**
         * Load variables from a file
         * @param fileName The file to use
         */
        protected void readVariablesFromPropertyFile(String fileName) {
                File file = new File(fileName);
                if (!file.exists()) {
                        throw new RuntimeException("The variable file " + file.getAbsolutePath() + " can't be found.");
                }
                logger.config("Reading variables from : " + file.getAbsolutePath());
                try {
                        FileInputStream input = new FileInputStream(fileName);
                        Scanner scanner = new Scanner(input);
                        int lineNumber = 1;
                        while (scanner.hasNextLine()) {
                                String line = scanner.nextLine();
                                if (!line.startsWith("#")) {
                                        int index = line.indexOf("=");
                                        if (index < 1) {
                                                logger.fine("Disregarding line " + lineNumber + " in " + file.getAbsolutePath() + " due to missing = character");
                                        } else {
                                                String key = line.substring(0, index);
                                                String value = line.substring(index + 1);
                                                logger.finer(key + ": " + value);
                                                customVariables.put(key, value);
                                        }
                                }
                                lineNumber++;
                        }
                } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                }
        }


                                /**
                                 * toString
                                 * @return A String representation of this configuration
                                 */
        public String toString() {
                StringBuilder sb = new StringBuilder();
                parameters.forEach((key, value) ->
                        sb.append(key.longName)
                        .append(":  ")
                        .append(value).append(System.lineSeparator()));
                return sb.toString();
        }
}
