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

package nu.sitia.loggenerator.filter;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nu.sitia.loggenerator.Configuration;
import nu.sitia.loggenerator.inputitems.UDPInputItem;
import nu.sitia.loggenerator.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class JsonFilter extends AbstractProcessFilter  {
    static final Logger logger = Logger.getLogger(JsonFilter.class.getName());

    /** path to the json object we will retrieve */
    private String path;

    /**
     * Create a RegexFilter and set all parameters
     * @param config The configuration
     */
    public JsonFilter(Configuration config) {
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("JsonFilter. Filter out all messages not matching a json path\n" +
                    "Parameters:\n" +
                    "--path <path> (-p <path>)\n" +
                    "  The path to the json object to retrieve\n");
            System.exit(1);
        }
        if (key != null && (key.equalsIgnoreCase("--path") || key.equalsIgnoreCase("-p"))) {
            this.path = value;
            logger.fine("path " + value);
            return true;
        }
        return false;
    }

    @Override
    public boolean afterPropertiesSet() {
        if (path == null) {
            throw new RuntimeException("Missing --path parameter");
        }
        return true;
    }

    /**
     * Filter one string
     *
     * @param toFilter The string to change
     * @return toFilter with a header added before the string.
     */
    private List<String> filter(String toFilter) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(toFilter);
            return new JsonUtil().matchPath(rootNode, this.path);
        } catch (Exception e) {
            // Ignore. This is not JSON
            return List.of(toFilter);
        }
    }

    @Override
    public List<String> filter(List<String> toFilter) {
        List<String> filtered = new ArrayList<>();
        toFilter.forEach(s ->
                filtered.addAll(filter(s)));
        return filtered;
    }

    /**
     * The current configuration
     * @return A printout of the current configuration
     */
    @Override
    public String toString() {
        return "JsonFilter: " + path + System.lineSeparator();
    }
}
