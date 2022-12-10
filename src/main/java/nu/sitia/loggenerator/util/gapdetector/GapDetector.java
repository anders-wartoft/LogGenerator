package nu.sitia.loggenerator.util.gapdetector;

import java.util.LinkedList;
import java.util.List;

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
            return 0;
        } else if (nextNumber < expectedNumber) {
            // Check if we have received this before
            Gap gap = isInGap(nextNumber);
            if (gap != null) {
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
            }
            return nextNumber - expectedNumber;
        } else {
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
}
