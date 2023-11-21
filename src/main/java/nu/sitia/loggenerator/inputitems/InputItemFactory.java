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

        if (input.equalsIgnoreCase("template")) return new TemplateFileInputItem(config);
        // special case. the FileInputItem needs to be able to send begin- and end messages with the filename
        if (input.equalsIgnoreCase("file")) return getFileInputItem(inputName, config);
        if (input.equalsIgnoreCase("json-file")) return new JsonFileInputItem(inputName, config);
        if (input.equalsIgnoreCase("udp")) return new UDPInputItem(config);
        if (input.equalsIgnoreCase("tcp-ssl")) return new SSLTCPInputItem(config);
        if (input.equalsIgnoreCase("tcp")) return new TCPInputItem(config);
        if (input.equalsIgnoreCase("kafka")) return new KafkaInputItem(config);
        if (input.equalsIgnoreCase("elastic")) return new ElasticInputItem(config);
        if (input.equalsIgnoreCase("static")) return new StaticInputItem(config);
        if (input.equalsIgnoreCase("counter")) return new CounterInputItem(config);
        throw new RuntimeException("Illegal input type: " + input);
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
