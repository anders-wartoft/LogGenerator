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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nu.sitia.loggenerator.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class JsonFileInputItem extends FileInputItem {
    /** If false, start with sending guard message if statistics is enabled */
    protected boolean initialized = false;

    /** Should we add statistics messages to the file contents? */
    protected boolean isStatistics;

    /** Should the file content be sent as is, or should a field be extracted? */
    protected String path;

    private List<String> contents = new ArrayList<>();

    /**
     * Create a new JsonFileInputItem
     */
    public JsonFileInputItem(Configuration config) {
        super(config);
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("JsonFileInputItem. Read a JSON file\n" +
                    "Parameters:\n" +
                    "--name <name> (-n <name>)\n" +
                    "  The name of the file to read\n" +
                    "--path <path> (-p <path>)\n" +
                    "  The JSON path to the field to extract\n");
            System.exit(1);
        }
        if(super.setParameter(key, value)) {
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--path") || key.equalsIgnoreCase("-p"))) {
            this.path = value;
            logger.fine("path " + value);
            return true;
        }
        return false;
    }


    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
        // The JSON file should be consumed in total and a JSON
        // library be used to get the contents
        File file = new File(fileName);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + file.getAbsolutePath());
        }
        try {
            //read json file data to String
            byte[] jsonData = Files.readAllBytes(Paths.get(fileName));

            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();

            //read JSON like DOM Parser
            JsonNode rootNode = objectMapper.readTree(jsonData);
            if (this.path != null) {
                this.contents = new nu.sitia.loggenerator.util.JsonUtil().matchPath(rootNode, this.path);
            } else {
                List<String> result = new ArrayList<>();
                result.add(rootNode.toString());
                this.contents = result;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Are there more messages to read?
     * @return True iff there are more messages
     */
    public boolean hasNext() {
        return this.contents.size() > 0;
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
        int lines = this.batchSize;
        int toRead = Math.min(lines, contents.size());
        while (toRead-- > 0) {
            String event = contents.remove(0);
            result.add(event);
        }
        if (contents.size() == 0 && isStatistics) {
            // End of file
            result.add(Configuration.END_FILE_TEXT + fileName);
        }
        return result;
    }

    /**
     * Let the item teardown after reading
     */
    public void teardown() {
    }

    /**
     * Make adding two items to a list depending on the id only
     * @param o Other object
     * @return true Iff the id of the two items are identical, and not nul.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonFileInputItem that = (JsonFileInputItem) o;
        return fileName.equals(that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName);
    }

    /**
     * Debug printouts (logger)
     * @return The filename for this item
     */
    public String toString() {
        return "JsonFileInputItem" + System.lineSeparator() +
                this.fileName;
    }
}
