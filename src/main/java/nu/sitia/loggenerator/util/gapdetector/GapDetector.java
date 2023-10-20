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

package nu.sitia.loggenerator.util.gapdetector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

public class GapDetector {

    /** Next number that we expect */
    private long expectedNumber = 1;

    /** All gaps. Ordered list. */
    private final List<Gap> gaps = new LinkedList<>();

    /** Test usage */
    protected void addGap(Gap gap) {
        gaps.add(gap);
    }

    /** Test usage */
    protected void setExpectedNumber(long newNumber) {
        expectedNumber = newNumber;
    }

    /** Should we also keep track of duplicate values? */
    protected boolean duplicateDetection = false;

    /** The list of doubles and how many times we have seen them */
    protected Map<Long, Long> duplicates = new LinkedHashMap<>();

    /** Number of unique received numbers */
    protected Long nrReceived = 0L;

    /**
     * We have received a number. Check if this number is
     * the next we anticipate.
     * @param nextNumber The number to check
     * @return 0 iff the supplied argument is the expected one.
     * If the argument is less than expected and the value hasn't
     * been seen before then 0 is returned. If the value is already
     * received then a negative value is returned.
     * If the argument is higher than the expected
     * value, then a positive value is returned and the internal
     * gap list is updated.
     */
    public long check(long nextNumber) {
        if (nextNumber == expectedNumber) {
            // This is what we want
            expectedNumber++;
            nrReceived++;
            return 0;
        } else if (nextNumber < expectedNumber) {
            // Check if we have received this before
            Gap gap = isInGap(nextNumber);
            if (gap != null) {
                nrReceived++;
                if (nextNumber == gap.getFrom() && nextNumber == gap.getTo()) {
                    // This gap covers exactly the number. Just remove this gap
                    gaps.remove(gap);
                } else if (nextNumber == gap.getFrom()) {
                    // NextNumber is the same as the starting number
                    gap.setFrom(nextNumber+1);
                } else if (nextNumber == gap.getTo()) {
                    gap.setTo(nextNumber-1);
                } else {
                    // Split into two gaps to remove the received number
                    Gap gap1 = new Gap(gap.getFrom(), nextNumber - 1);
                    Gap gap2 = new Gap(nextNumber + 1, gap.getTo());
                    int position = gaps.indexOf(gap);
                    gaps.add(position+1, gap1);
                    gaps.add(position+2, gap2);
                    gaps.remove(gap);
                }
            } else if (duplicateDetection) {
                // nextNumber < expectedNumber but not in any gaps,
                // so we have seen this before
                Long cached = duplicates.get(nextNumber);
                if (cached == null) {
                    // Now we have seen this number twice
                    cached = 2L;
                } else {
                    cached++;
                }
                duplicates.put(nextNumber, cached);
            }
            return nextNumber - expectedNumber;
        } else {
            nrReceived++;
            // gap detected
            Gap gap = new Gap(expectedNumber, nextNumber-1);
            gaps.add(gap);
            long result = nextNumber - expectedNumber;
            expectedNumber = nextNumber + 1;
            return (result);
        }
    }

    /**
     * Check if the supplied number is in a gap list. If so, return that gap
     * @param number The number to check
     * @return The gap the number is in, or null if not found
     */
    private Gap isInGap(long number) {
        for(Gap gap: gaps) {
            if (gap.getFrom() <= number && gap.getTo() >= number) {
                return gap;
            }
            if (number < gap.getTo()) {
                // Ordered list, so we know this is not in any gap
                return null;
            }
        }
        return null;
    }

    public void setDuplicateDetection(boolean dd) {
        this.duplicateDetection = dd;
    }

    /**
     * Sort the duplicates map
     * @param toSort A map to sort
     * @return A map sorted on the keys in ascending order
     */
    private Map<Long, Long> getSorted(Map<Long, Long>toSort) {
        List<Map.Entry<Long, Long>> entries = new ArrayList<>(toSort.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        Map<Long, Long> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    /**
     * Return all gaps as a printable string
     * @return The gaps, one gap for each line
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (gaps.size() > 0) {
            sb.append("Gaps found: ").append(gaps.size()).append(".").append(System.lineSeparator());
            for (Gap gap : gaps) {
                sb.append(gap.getFrom()).append("-").append(gap.getTo()).append(System.lineSeparator());
            }
        }
        if (duplicateDetection) {
            sb.append("Duplicate detection found: ").append(duplicates.size()).append(" duplicate (or more) values.").append(System.lineSeparator());
            Map<Long, Long> sortedMap = getSorted(duplicates);
            sortedMap.forEach((key, value) -> sb.append(key).append(" - ").append(value).append(System.lineSeparator()));
        }
        sb.append("Number of unique received numbers: ").append(nrReceived).append(System.lineSeparator());
        sb.append("Next expected number: ").append(expectedNumber).append(System.lineSeparator());
        return sb.toString();
    }

    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        ArrayNode gapArray = result.putArray("gaps");

        for (Gap gap: this.gaps) {
            ObjectNode gapNode = mapper.createObjectNode();
            gapNode.put("from", gap.getFrom());
            gapNode.put("to", gap.getTo());
            gapArray.add(gapNode);
        }

        result.put("unique", this.nrReceived);

        Map<Long, Long> sortedMap = getSorted(this.duplicates);
        ArrayNode duplicateList = result.putArray("duplicates");

        for (Map.Entry<Long, Long> entry : this.duplicates.entrySet()) {
            ObjectNode duplicateNode = mapper.createObjectNode();
            duplicateNode.put(entry.getKey().toString(), entry.getValue());
            duplicateList.add(duplicateNode);
        }

        result.put("next", this.expectedNumber);
        ObjectWriter ow = new ObjectMapper().writer();
        try {
            String json = ow.writeValueAsString(result);
            return json;
        } catch (Exception e) {
            return this.toString();
        }
    }
}
