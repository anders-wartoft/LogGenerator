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
import nu.sitia.loggenerator.filter.FilterListFactory;
import nu.sitia.loggenerator.filter.ProcessFilter;
import nu.sitia.loggenerator.inputitems.InputItem;
import nu.sitia.loggenerator.inputitems.InputItemFactory;
import nu.sitia.loggenerator.outputitems.OutputItem;
import nu.sitia.loggenerator.outputitems.OutputItemFactory;


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
        // Create a list of filters
        List<ProcessFilter> filterList = new FilterListFactory().create(args);

        // Now create the input and output items
        InputItem inputItem = InputItemFactory.create(args);
        OutputItem outputItem = OutputItemFactory.create(args);

        // And the proxy that mediates the flow
        ItemProxy proxy = new ItemProxy(inputItem, outputItem, filterList, args);

        // Print the configuration for every object
        logger.config(inputItem.toString());
        filterList.forEach(s -> logger.config(s.toString()));
        logger.config(outputItem.toString());

        // Start pumping messages
        proxy.pump();
    }
}
