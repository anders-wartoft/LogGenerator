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

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DirectoryInputItem extends AbstractInputItem {
    static final Logger logger = Logger.getLogger(DirectoryInputItem.class.getName());

    /** The name of the directory this item will read from */
    private final String directoryName;

    /** The files in the directory to process */
    private final List<FileInputItem> fileList = new ArrayList<>();

    /** The file name glob to use */
    private final String glob;

    /** The current file we are using */
    private FileInputItem fileItem = null;

    /**
     * Create a new FileInputItem
     * @param config The command line arguments
     */
    public DirectoryInputItem(Configuration config) {
        super(config);
        this.directoryName = config.getValue("-ifn");
        if (directoryName == null) {
            throw new RuntimeException(config.getNotFoundInformation("-ifn"));
        }

        logger.fine("Creating DirectoryInputItem: " + directoryName);

        String globString = config.getValue("-g");

        if (null == globString) {
            glob = "glob:**";
        } else {
            glob = "glob:" +globString;
        }

        logger.fine("setup " + glob);
        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(glob);
        try {
            Files.walkFileTree(Paths.get(directoryName), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path path,
                                                 BasicFileAttributes attrs) {
                    if (pathMatcher.matches(path)) {
                        logger.fine("Adding " + path.toString());
                        FileInputItem fi = new FileInputItem(path.toString(), config);
                        fileList.add(fi);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
    }

    /**
     * Are there more messages to read?
     * @return True iff there are more messages
     */
    public boolean hasNext() {
        // We have reached the end of all files
        return fileItem != null || fileList.size() != 0;
    }

    /**
     * Read input one line at a time and return
     * {batchSize} elements.
     * @return The read input
     */
    public List<String> next() {
        List<String> result = new ArrayList<>();
        if (fileItem == null) {
            // First file
            fileItem = fileList.get(0);
            fileList.remove(fileItem);
            logger.finer("Running " + fileItem.toString());
            fileItem.setup();
        }
        int lines = this.batchSize;
        while (fileItem.hasNext() && lines-- > 0) {
            result.addAll(fileItem.next());
        }
        if (!fileItem.hasNext()) {
            fileItem.teardown();
            fileItem = null;
        }
        return result;
    }

    /**
     * Let the item teardown after reading
     */
    public void teardown() {
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DirectoryInputItem").append(System.lineSeparator());
        sb.append(directoryName).append(System.lineSeparator());
        sb.append(glob).append(System.lineSeparator());
        fileList.forEach(s -> sb.append(s.toString()).append(System.lineSeparator()));
        return sb.toString();
    }
}
