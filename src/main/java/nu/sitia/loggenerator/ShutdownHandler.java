package nu.sitia.loggenerator;

public interface ShutdownHandler {
    /**
     * The app is exiting...
     * Take this software down gently
     */
    void shutdown();
}
