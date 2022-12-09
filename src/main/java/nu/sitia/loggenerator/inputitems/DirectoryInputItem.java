package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.util.Configuration;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DirectoryInputItem extends AbstractInputItem {
    static Logger logger = Logger.getLogger(DirectoryInputItem.class.getName());

    /** The name of the directory this item will read from */
    private final String directoryName;

    /** The configuration object */
    private final Configuration config;

    /** The files in the directory to process */
    private final List<FileInputItem> fileList = new ArrayList<>();

    /** The file name glob to use */
    private final String glob;

    /** The current file we are using */
    private FileInputItem fileItem = null;

    /**
     * Create a new FileInputItem
     * @param config The Configuration object
     */
    public DirectoryInputItem(Configuration config) {
        this.directoryName = config.getInputName();
        logger.fine("Creating DirectoryInputItem: " + directoryName);
        this.config = config;
        if (null == config.getGlob()) {
            this.glob = "glob:**";
        } else {
            this.glob = "glob:" + config.getGlob();
        }
    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
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
}
