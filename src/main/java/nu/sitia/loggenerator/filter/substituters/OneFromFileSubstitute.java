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

package nu.sitia.loggenerator.filter.substituters;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OneFromFileSubstitute extends AbstractSubstitute {

    /** Regex for oneOf */
    private static final String oneFromFileRegex = "\\{oneFromFile:(?<filename>[^#]+)(#(?<encoding>.*))?\\}";

    /** Cached pattern for getting oneFromFile */
    private static final Pattern oneFromFilePattern = Pattern.compile(oneFromFileRegex);

    /** Cached files */
    private static Map<String, List<String>> cache = new HashMap<>();

    /**
     * Get one of several options from a file.
     * {oneFromFile:file.txt} will randomly pick one line from the file: file.txt and display.
     * @param input The string containing the filename specification
     * @return The input but with one of the lines from the file instead of the specification
     */
    public String substitute(String input) {
        // Since Java doesn't have recursive regexes, we search for the end of the expression manually:
        int startPos = input.indexOf("{oneFromFile:");
        if (startPos < 0) return input;

        int endPos = getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the choices
        Matcher matcher = oneFromFilePattern.matcher(part);
        if (matcher.find()) {
            String fileName = matcher.group("filename");
            String encoding = Charset.defaultCharset().name();
            if (matcher.group("encoding") != null) {
                // Encoding
                encoding = matcher.group("encoding");
            }
            List<String> content = getContent(fileName, encoding);
            int nr = new Random().nextInt(content.size());
            String selected = content.get(nr);
            return input.substring(0, startPos) + selected + input.substring(endPos);
        }
        throw new RuntimeException(("Illegal oneFromFile pattern: " + input));
    }

    /**
     * Get the cached file contents. If the file is not loaded yet,
     * load the file and save the contents in the cache.
     * @param fileName
     * @return
     */
    private List<String> getContent(String fileName, String encoding) {
        if (cache.containsKey(fileName)) {
            return cache.get(fileName);
        }
        List<String> contents = getFileContent(fileName, encoding);
        cache.put(fileName, contents);
        return contents;
    }


    /**
     * Read a file from disk and return the values as a list of lines
     * @param fileName The file name
     * @param encoding The encoding to use (charset)
     * @return A List of lines
     */
    private List<String> getFileContent(String fileName, String encoding) {

        File file = new File(fileName);
        if (!file.exists()) {
            throw new RuntimeException("File: " + file.getAbsolutePath() + " not found");
        }
        try (
            BufferedReader buffReader = Files.newBufferedReader(Paths.get(fileName), Charset.forName(encoding)); ) {
            List<String> content = new LinkedList<>();
            return buffReader.lines().collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
