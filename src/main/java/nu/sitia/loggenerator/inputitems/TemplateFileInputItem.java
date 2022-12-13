package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.filter.substituters.Substitution;
import nu.sitia.loggenerator.util.Configuration;

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
public class TemplateFileInputItem extends AbstractInputItem {
    /** The name of the file this item will read from */
    private final String fileName;

    /** The scanner to read from. Initialized in setup() */
    private Scanner scanner = null;

    /** The config object */
    private final Configuration config;

    /** Describes how this item should function */
    private final Configuration.Template template;

    /** If false, start with sending guard message if statistics is enabled */
    private boolean initialized = false;

    /** The lines from the file */
    private final List<String> rows = new ArrayList<>();

    /** End time (used for template time:{seconds} ) */
    private final long endTime;

    /**
     * Create a new FileInputItem
     * @param config The Configuration object
     */
    public TemplateFileInputItem(Configuration config) {
        super(config);
        this.fileName = config.getInputName();
        this.config = config;
        this.template = config.getTemplate();
        if (template == Configuration.Template.TIME) {
            // Set last time we are allowed to process input
            this.endTime = new Date().getTime() + config.getDurationMilliSeconds();
        } else {
            this.endTime = 0;
        }
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
            if (config.isStatistics()) {
                result.add(Configuration.BEGIN_FILE_TEXT + fileName);
            }
        }
        if (this.endTime > 0) {
            // We are limited in time (for example with flag -t time:1000)
            long now = new Date().getTime();
            if (now > this.endTime) {
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
            if (template != Configuration.Template.NONE) {
                // Do translation
                result.add(substitution.substitute(line, new HashMap<>(), new Date()));
            } else {
                result.add(line);
            }

            // Should we remove the line? In that case the file content will be
            // sent only once
            if (template == Configuration.Template.FILE ||
                    template == Configuration.Template.NONE) {
                rows.remove(lineNr);
            }
        }

        if (rows.size() == 0 && config.isStatistics()) {
            // End of file
            result.add(Configuration.END_FILE_TEXT + fileName);
        }
        return result;
    }

    /**
     * Let the item teardown after reading
     */
    public void teardown() {
        scanner.close();
        // make sure we have to run setup() again before read()
        scanner = null;
    }

    /**
     * Debug printouts (logger)
     * @return The filename for this item
     */
    public String toString() {
        return this.fileName;
    }
}
