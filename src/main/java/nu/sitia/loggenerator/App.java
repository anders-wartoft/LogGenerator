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


import java.util.List;
import java.util.logging.Logger;

/**
 * Command line interface to the log generator
 *
 */
public class App 
{
    static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main( String[] args )
    {
        // The config knows how to get parameters from the user
        Configuration config = new Configuration(args);

        // Print the parameters
        logger.config(config.toString());

        // Create a list of items
        List<ProcessItem> itemList = new ProcessItemListFactory().create(config);

        // And the proxy that mediates the flow
        ItemProxy proxy = new ItemProxy(itemList, config);

        // Print the configuration for every object
//        itemList.forEach(s -> logger.config(s.toString()));

        // Start pumping messages
        proxy.pump();
    }
}
