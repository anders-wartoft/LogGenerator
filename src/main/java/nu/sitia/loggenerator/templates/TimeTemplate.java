package nu.sitia.loggenerator.templates;

public class TimeTemplate extends AbstractTemplate {

    private long time;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public TimeTemplate(long time) {
        this.time = time;
    }

    @Override
    public boolean isTime() {
        return true;
    }
}
