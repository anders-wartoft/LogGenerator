package nu.sitia.loggenerator.templates;

public abstract class AbstractTemplate implements Template {
    @Override
    public boolean isTime() {
        return false;
    }

    @Override
    public boolean isNone() {
        return false;
    }

    @Override
    public boolean isFile() {
        return false;
    }

}
