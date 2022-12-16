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

package nu.sitia.loggenerator.outputitems;

import nu.sitia.loggenerator.Configuration;


public class OutputItemFactory {

    /**
     * Create an OutputItem depending on the configuration
     * @param config The command line arguments to use to create an OutputItem
     * @return An OutputItem to use
     */
    public static OutputItem create(Configuration config) {
        String outputType = config.getValue("-o");
        if (null == outputType) {
            throw new RuntimeException(config.getNotFoundInformation("-o"));
        }

        return switch (outputType) {
            case "cmd", "CMD" -> new CmdOutputItem(config);
            case "udp", "UDP" -> new UDPOutputItem(config);
            case "tcp", "TCP" -> new TCPOutputItem(config);
            case "tcp-ssl", "TCP-SSL" -> new SSLTCPOutputItem(config);
            case "file", "FILE" -> new FileOutputItem(config);
            case "kafka", "KAFKA" -> new KafkaOutputItem(config);
            case "null", "NULL" -> new NullOutputItem(config);
            default -> throw new RuntimeException("Illegal output type: " + outputType);
        };
    }
}
