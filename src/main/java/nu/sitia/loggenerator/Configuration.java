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

        /** Should we send statistics messages so that timers can function? */
        private boolean statistics = false;

        /** Limit events per second. Zero means no limit */
        private double eps = 0;

        /** Limit number of emitted events. After the limit is reached, the program will terminate.
         * Zero means no limit */
        private long limit = 0;

        static {
            BEGIN_TRANSACTION.add(BEGIN_TRANSACTION_TEXT);
            BEGIN_FILE.add(BEGIN_FILE_TEXT);
            END_TRANSACTION.add(END_TRANSACTION_TEXT);
            END_FILE.add(END_FILE_TEXT);
        }


        /** A list of valid parameter keys */
        static final List<Item> keys = new ArrayList<>();

        static {
                keys.add(new Item("-h", "--help", "Show the help information"));
                keys.add(new Item("-i", "--input", "The input module to use. Valid arguments are: counter, file, kafka, elastic, static, tcp, udp, json-file and template"));
                keys.add(new Item("-o", "--output", "The output module to use. Valid arguments are: cmd, file, kafka, null, elastic, tcp and udp"));
                keys.add(new Item("-f", "--filter", "The filter module to use. Valid arguments are: drop, gap, guard, header, json, regex, select and substitute"));
                keys.add(new Item("-pf", "--property-file", "The property file to use. A property file can contain all valid arguments in a specific order"));
                keys.add(new Item("-vf", "--variable-file", "The variable file to use. A variable file can contain all variables to use in a specific order"));
                keys.add(new Item("-l", "--limit", "Limit the number of events to send. Zero means no limit"));
                keys.add(new Item("-e", "--eps", "Limit the number of events per second. Zero means no limit"));
                keys.add(new Item("-s", "--statistics", "Send statistics messages so that timers can function. Valid arguments are: true or false"));
        }

        static final Map<String, String> standardVariables = new HashMap<>();
        static {
                standardVariables.put("syslog-header", "<{pri:}>{date:MMM dd HH:mm:ss} {oneOf:my-machine,your-machine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{random:1-65535}]: ");
                standardVariables.put("ip", "{<ipv4:0.0.0.0/0}");
                standardVariables.put("rfc1918","{oneOf:{ipv4:192.168.0.0/16},{ipv4:172.16.0.0/12},{ipv4:10.0.0.0/8}}");
                standardVariables.put("ipv6", "{repeat:8#{string:0-9a-f/4}#-#}");
                standardVariables.put("hex", "{string:0-9a-f/1}");
                standardVariables.put("HEX", "{string:0-9A-F/1}");
                standardVariables.put("guid", "{string:0-9a-f/8}-{repeat:3#{string:0-9a-f/4}-}{string:0-9a-f/12}");
                standardVariables.put("GUID", "{string:0-9A-F/8}-{repeat:3#{string:0-9A-F/4}-}{string:0-9A-F/12}");

        }

        final Map<String, String> customVariables = new HashMap<>();

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
        private final KvList parameters = new KvList();

        /** Get the parameters as a raw map */
        public KvList getParameters() {
                return parameters;
        }


        /**
         * Get a configuration value
         * @param string A string like --help
         * @return A value from the configuration for that key, or null if not found
         */
        public String getValue(String string) {
                return parameters.get(string);
        }

        /**
         * Print help information
         */
        private void printHelp() {
                System.out.println("Arguments: ");
                for (Item key:keys) {
                        System.out.println(key.getShortName() + " " + key.getLongName() + " " + key.getDescription());
                }
        }

        private void parseArgs(String [] args) {
                for (int i=0; i<args.length; i+=2) {
                        String key = args[i];
                        String value = null;
                        if (args.length <= 2 && key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
                                printHelp();
                                System.exit(0);
                        }
                        if (i + 1 < args.length) {
                                value = args[i+1];
                        } else if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))){
                                // No value needed for help
                                value = "";
                        } else {
                                printHelp();
                                System.out.println("Missing parameter for argument: " + key);
                                System.exit(1);
                        }
                        if (key != null && (key.equalsIgnoreCase("--property-file") || key.equalsIgnoreCase("-pf"))) {
                                // Read the configuration from the property file
                                KvList list = readConfigFromPropertyFile(value);
                                List<String> params = new ArrayList<>();
                                list.list.forEach(item-> {
                                        String newKey = item.getKey();
                                        String newValue = item.getValue();
                                        params.add(newKey);
                                        params.add(newValue);
                                });
                                parseArgs(params.toArray(new String[0]));
                        } else if (key != null && (key.equalsIgnoreCase("--variable-file") || key.equalsIgnoreCase("-vf"))) {
                                // Read variables from the property file
                                readVariablesFromPropertyFile(value);
                        } else if (key != null && (key.equalsIgnoreCase("--limit") || key.equalsIgnoreCase("-l"))) {
                                // Only send (limit) number of events
                                setLimit(value);
                        } else if (key != null && (key.equalsIgnoreCase("--eps") || key.equalsIgnoreCase("-e"))) {
                                // Limit events per second
                                setEps(value);
                        } else if (key != null && (key.equalsIgnoreCase("--statistics") || key.equalsIgnoreCase("-s"))) {
                                // Read variables from the property file
                                statistics = value.equalsIgnoreCase("true");
                        } else {
                                parameters.add(key, value);
                        }

                }
        }

        /**
         * Create a new configuration object and get the parameters from the user
         * @param args The command line arguments
         */
        public Configuration(String [] args) {
                parseArgs(args);
        }

        private void setLimit(String l) {
                this.limit = Long.parseLong(l);
        }

        private void setEps(String e) {
                this.eps = Double.parseDouble(e);
        }
        public long getLimit() {
                return limit;
        }
        public double getEps() {
                return eps;
        }

        /**
         * Load configuration from a file
         * @param fileName The file to use
         */
        protected KvList readConfigFromPropertyFile(String fileName) {
                KvList list = new KvList();
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
                                                } else if (key.equalsIgnoreCase("variable-file")) {
                                                        // Read variables from the property file
                                                        readVariablesFromPropertyFile(value);
                                                } else {
                                                        list.add("--" + key, value);
                                                }
                                        }
                                }
                                lineNumber++;
                        }
                } catch (IOException e) {
                        throw new RuntimeException("Exception trying to load file: " + file.getAbsolutePath(), e);
                }
                return list;
        }

        /**
         * Should statistics messages and outputs be used?
         * @return true Iff statistics is enabled
         */
        public boolean isStatistics() {
                return statistics;
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
                parameters.list.forEach(item ->
                        sb.append(item.getKey())
                        .append(":  ")
                        .append(item.getValue()).append(System.lineSeparator()));
                return sb.toString();
        }
}
