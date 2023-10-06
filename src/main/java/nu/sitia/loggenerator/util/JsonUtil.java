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

package nu.sitia.loggenerator.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

public class JsonUtil {

    private List<String> getArrayItems(JsonNode arrayNode) {
        ArrayNode array = (ArrayNode) arrayNode;
        List<String> result = new ArrayList<>();
        for(JsonNode x: array) {
            if (x.isObject()) {
                result.add(x.toString());
            } else if (x.isArray()) {
                result.add(x.toString());
            } else {
                result.add(x.asText());
            }
        }
        return result;
    }

    public List<String> matchPath(JsonNode input, String path) {
        String[] paths = path.split("\\.", 2);
        String toMatch = paths[0];
        if (input.has(toMatch)) {
            JsonNode matchedNode = input.get(toMatch);
            if (paths.length > 1) {
                return matchPath(matchedNode, paths[1]);
            }
            if (matchedNode.isArray()) {
                return getArrayItems(matchedNode);
            } else if (matchedNode.isObject()) {
                // Don't use List.of here. That's immutable
                List<String>toReturn = new ArrayList<>();
                toReturn.add(matchedNode.toString());
                return toReturn;
            } else {
                // Don't use List.of here. That's immutable
                List<String>toReturn = new ArrayList<>();
                toReturn.add(matchedNode.asText());
                return toReturn;
            }
        }
        // No match
        // Don't use List.of here. That's immutable
        List<String>toReturn = new ArrayList<>();
        toReturn.add(input.toString());
        return toReturn;
    }
}
