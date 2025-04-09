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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class KvList {
    List<KV> list = new ArrayList<>();

    public class KV {
        public KV(String k, String v) {
            this.key = k;
            this.value = v;
        }
        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
    public void add(String k, String v) {
        list.add(new KV(k, v));
    }
    public Iterator<KV> iterator() {
        return list.iterator();
    }
    public String get(String k) {
        KV result = list.stream().reduce(null, (a, s) -> s.getKey().equalsIgnoreCase(k) ? s : a);
        if (result != null) {
            return result.getValue();
        }
        return null;
    }

    public void putAll(KvList other) {
        other.list.forEach(item -> list.add(item));
    }

    public int size() {
        return list.size();
    }
}
