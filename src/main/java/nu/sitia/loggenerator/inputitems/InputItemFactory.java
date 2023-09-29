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

import nu.sitia.loggenerator.Configuration;


import java.io.File;

public class InputItemFactory {

    /**
     * Create an InputItem depending on the configuration
     * @param config The command line arguments to use to create an InputItem
     * @return An InputItem to use
     */
    public static InputItem create(Configuration config) {
        String input = config.getValue("-i");
        String inputName = config.getValue("-ifn");
        if (null == input) {
            throw new RuntimeException(config.getNotFoundInformation("-i"));
        }

        return switch (input) {
            case "template", "TEMPLATE" -> new TemplateFileInputItem(config);
            // special case. the FileInputItem needs to be able to send begin- and end messages with the filename
            case "file", "FILE" -> getFileInputItem(inputName, config);
            case "udp", "UDP" -> new UDPInputItem(config);
            case "tcp-ssl", "TCP-SSL" -> new SSLTCPInputItem(config);
            case "tcp", "TCP" -> new TCPInputItem(config);
            case "kafka", "KAFKA" -> new KafkaInputItem(config);
            case "elastic", "ELASTIC" -> new ElasticInputItem(config);
            case "static", "STATIC" -> new StaticInputItem(config);
            case "counter", "COUNTER" -> new CounterInputItem(config);
            default -> throw new RuntimeException("Illegal input type: " + input);
        };
    }

    /**
     * Helper method to return a FileInputItem or a DirectoryInputItem
     * @param name The name of the item
     * @param config The command line arguments object
     * @return A File or Directory input item
     */
    private static InputItem getFileInputItem(String name, Configuration config) {
        if (name == null) {
            throw new RuntimeException(config.getNotFoundInformation("-ifn"));
        }

        File file = new File(name);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + file.getAbsolutePath());
        }
        if (file.isDirectory()) {
            return new DirectoryInputItem(config);
        }
        return new FileInputItem(name, config);
    }
}
