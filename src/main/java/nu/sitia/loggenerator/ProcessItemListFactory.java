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

import nu.sitia.loggenerator.filter.FilterItemFactory;
import nu.sitia.loggenerator.inputitems.InputItemFactory;
import nu.sitia.loggenerator.outputitems.OutputItemFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class ProcessItemListFactory {
    static final Logger logger = Logger.getLogger(ProcessItemListFactory.class.getName());

    public ProcessItemListFactory() {

    }

    public List<ProcessItem> create(Configuration config) {
        List<ProcessItem> items = new LinkedList<>();
        ProcessItem lastItem = null;
        for (Iterator<KvList.KV> iterator = config.getParameters().iterator(); iterator.hasNext(); ) {
            KvList.KV kv = (KvList.KV) iterator.next();
            String key = kv.getKey();
            String value = kv.getValue();
            if (key != null && (key.equalsIgnoreCase("--input") || key.equalsIgnoreCase("-i"))) {
                // Load an input item
                lastItem = InputItemFactory.create(config, value);
                items.add(lastItem);
                logger.fine("Adding: " + value);
            } else if (key != null && (key.equalsIgnoreCase("--output") || key.equalsIgnoreCase("-o"))) {
                // Load an output item
                lastItem = OutputItemFactory.create(config, value);
                items.add(lastItem);
                logger.fine("Adding: " + value);
            } else if (key != null && (key.equalsIgnoreCase("--filter") || key.equalsIgnoreCase("-f"))) {
                // Load a filter item
                lastItem = FilterItemFactory.create(config, value);
                items.add(lastItem);
                logger.fine("Adding: " + value);
            } else {
                // Push this config to the last added item
                if (lastItem != null && !lastItem.setParameter(key, value)) {
                    // The item didn't accept the parameter
                    logger.finest("Pushing: " + key + " = " + value + " to " + lastItem.getClass().getSimpleName());
                    throw new RuntimeException("Invalid parameter: " + key + " - " + value + " " + lastItem.getClass().getSimpleName());
                }
            }
        }
        items.forEach(i -> i.afterPropertiesSet());
        items.forEach(i -> logger.config(i.toString()));
        return items;
    }
}
