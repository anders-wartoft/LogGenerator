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

public class InputItemFactory {

    /**
     * Create an InputItem depending on the configuration
     * @param name The command line arguments to use to create an InputItem
     * @return An InputItem to use
     */
    public static InputItem create(Configuration config, String name) {
        if (name.equalsIgnoreCase("template")) return new TemplateFileInputItem(config);
        if (name.equalsIgnoreCase("file")) return new DirectoryInputItem(config);
        if (name.equalsIgnoreCase("json-file")) return new JsonFileInputItem(config);
        if (name.equalsIgnoreCase("udp")) return new UDPInputItem(config);
        if (name.equalsIgnoreCase("tcp-ssl")) return new SSLTCPInputItem(config);
        if (name.equalsIgnoreCase("tcp")) return new TCPInputItem(config);
        if (name.equalsIgnoreCase("kafka")) return new KafkaInputItem(config);
        if (name.equalsIgnoreCase("elastic")) return new ElasticInputItem(config);
        if (name.equalsIgnoreCase("static")) return new StaticInputItem(config);
        if (name.equalsIgnoreCase("counter")) return new CounterInputItem(config);
        if (name.equalsIgnoreCase("string")) return new StringInputItem(config);
        if (name.equalsIgnoreCase("relp")) return new RelpInputItem(config);
        throw new RuntimeException("Illegal input item type: " + name);
    }

}
