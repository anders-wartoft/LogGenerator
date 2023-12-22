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
import nu.sitia.loggenerator.Item;
import nu.sitia.loggenerator.filter.substituters.Substitution;
import nu.sitia.loggenerator.templates.Template;
import nu.sitia.loggenerator.templates.TemplateFactory;
import nu.sitia.loggenerator.templates.TimeTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * A TemplateFileInputItem is similar to a FileInputItem, but each row
 * in the file will be used as a template instead. The lines will be
 * loaded into an array and, if template is set to:
 * - continuous:
 * A random line will be selected, expanded and sent. The array will not
 * change so the next line may be the same as the previous. This is good
 * for generating a lot of logs.
 * - file:
 * The contents will be sent one line at a time, but all variables will
 * be expanded (like {date:...}, {oneOf:...} etc).
 */
public class TemplateFileInputItem extends FileInputItem {
    /** Describes how this item should function */
    private Template template;

    /** The lines from the file */
    private final List<String> rows = new ArrayList<>();

    /** Offset from the current time and date to use when evaluating variables */
    private long timeOffset = 0;

    /** The offset as a String */
    private String offset = "0";

    /** The cached substitution handler */
    final Substitution substitution = new Substitution();


    /**
     * Create a new TemplateFileInputItem
     */
    public TemplateFileInputItem(Configuration config) {
        super(config);
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("TemplateFileInputItem. Load a file as a template and resolve variables before sending.\n" +
                    "Parameters:\n" +
                    "--name <name> (-n <name>)\n" +
                    "  The name of the file to read\n" +
                    "--template <template> (-t <template>)\n" +
                    "  The template to use. One of: none, file, time:{number} or continuous\n" +
                    "  none: Send the file without expanding the variables but in a random order\n" +
                    "  continuous: Send a random row from the file with variables expanded. The same row can be sent several times. If you just want a specified number of events, add the -l (--limit) parameter to stop sending after the specified number of events.\n" +
                    "  time:{number}: as 'continuous' but end the transmission after {number} ms\n" +
                    "  file: send the file in random order with variables expanded\n" +
                    "--time-offset <long value> (-to <long value>)\n" +
                    "  The offset in milliseconds to use when evaluating variables\n" +
                    "  Example: --time-offset -10000 for setting the date to 10 seconds ago.\n");
            System.exit(1);
        }
        if (super.setParameter(key, value)) {
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--template") || key.equalsIgnoreCase("-t"))) {
            template = TemplateFactory.getTemplate(value);
            logger.fine("template " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--time-offset") || key.equalsIgnoreCase("-to"))) {
            this.offset = value;
            logger.fine("timeOffset " + value);
            return true;
        }
        return false;
    }

    @Override
    public boolean afterPropertiesSet() {
        if (template == null) {
            throw new RuntimeException("Missing -template parameter");
        }
        try {
            this.timeOffset = Long.parseLong(offset);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Usage: --time-offset [long value]. Example: --time-offset -10000 for setting the date to 10 seconds ago.");
        }
        return true;
    }


    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
        FileInputStream input;
        try {
            input = new FileInputStream(fileName);
            scanner = new Scanner(input);
            while (scanner.hasNextLine()) {
                rows.add(scanner.nextLine());
            }

        } catch (FileNotFoundException e) {
            File file = new File(fileName);
            throw new RuntimeException("File not found: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Are there more messages to read?
     * @return True iff there are more messages
     */
    public boolean hasNext() {
        return rows.size() > 0;
    }

    /**
     * Read input one line at a time and return
     * {batchSize} elements.
     * @return The read input
     */
    public List<String> next() {
        List<String> result = new ArrayList<>();
        if (!initialized) {
            initialized = true;
            if (isStatistics) {
                result.add(Configuration.BEGIN_FILE_TEXT + fileName);
            }
        }
        if (template.isTime()) {
            // We are limited in time (for example with flag -t time:1000)
            long now = new Date().getTime();
            if (now > ((TimeTemplate)template).getTime()) {
                // We are done now. Make sure we won't continue but run the normal path
                rows.clear();
            }
        }

        int lines = this.batchSize;
        Random random = new Random();
        while (rows.size() > 0 && lines-- > 0) {
            // Pick one line. lineNr will be in the range [0-rows.size()-1]
            int lineNr = random.nextInt(rows.size());
            String line = rows.get(lineNr);
            if (!template.isNone()) {
                // Do translation
                // Create a new date that represents now. Then, add the
                // offset provided by the user (positive for future and negative for in the past
                final Date date = new Date(new Date().getTime() + this.timeOffset);
                result.add(substitution.substitute(line, new HashMap<>(), date));
            } else {
                result.add(line);
            }

            // Should we remove the line? In that case the file content will be
            // sent only once
            if (template.isFile() ||
                    template.isNone()) {
                rows.remove(lineNr);
            }
        }

        if (rows.size() == 0 && isStatistics) {
            // End of file
            result.add(Configuration.END_FILE_TEXT + fileName);
        }
        return result;
    }

    /**
     * Let the item teardown after reading
     */
    public void teardown() {
        if (scanner != null) {
            scanner.close();
            // make sure we have to run setup() again before read()
            scanner = null;
        }
    }

    /** Get the template */
    public Template getTemplate() {
        return template;
    }

    /**
     * Debug printouts (logger)
     * @return The filename for this item
     */
    public String toString() {
        return"TemplateFileInputItem" + System.lineSeparator() + this.fileName;
    }
}
