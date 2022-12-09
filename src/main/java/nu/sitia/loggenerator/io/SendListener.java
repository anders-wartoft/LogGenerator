package nu.sitia.loggenerator.io;

import java.util.List;

public interface SendListener {
    /**
     * Callback method. Called from AbstractOutputItem when
     * the cache should be sent. The items in the cache
     * are purged after the call.
     * @param toSend String to send
     */
     void send(List<String> toSend);
}
