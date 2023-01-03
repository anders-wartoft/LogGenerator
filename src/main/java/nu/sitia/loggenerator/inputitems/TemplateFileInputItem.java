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
    private final Template template;

    /** The lines from the file */
    private final List<String> rows = new ArrayList<>();

    /**
     * Create a new TemplateFileInputItem
     * @param config The command line arguments
     */
    public TemplateFileInputItem(Configuration config) {
        super(config.getValue("-ifn"), config);
        String templateString = config.getValue("-t");
        if (templateString == null) {
            throw new RuntimeException(config.getNotFoundInformation("-t"));
        }
        template = TemplateFactory.getTemplate(templateString);

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
        Substitution substitution = new Substitution();
        while (rows.size() > 0 && lines-- > 0) {
            // Pick one line. lineNr will be in the range [0-rows.size()-1]
            int lineNr = random.nextInt(rows.size());
            String line = rows.get(lineNr);
            if (!template.isNone()) {
                // Do translation
                result.add(substitution.substitute(line, new HashMap<>(), new Date()));
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

    /**
     * Debug printouts (logger)
     * @return The filename for this item
     */
    public String toString() {
        return"TemplateFileInputItem" + System.lineSeparator() + this.fileName;
    }
}
