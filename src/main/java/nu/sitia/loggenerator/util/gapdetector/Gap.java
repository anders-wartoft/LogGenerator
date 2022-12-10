package nu.sitia.loggenerator.util.gapdetector;

public class Gap {
    /** Start value of this gap */
    private long from;

    /** End value of this gap */
    private long to;

    /**
     * Constructor
     * @param from From value
     * @param to To value
     */
    public Gap(long from, long to) {
        this.from = from;
        this.to = to;
    }

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }
}
