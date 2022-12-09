package nu.sitia.loggenerator.outputitems;

import nu.sitia.loggenerator.util.Configuration;

public class OutputItemFactory {

    /**
     * Create an OutputItem depending on the configuration
     * @param config The configuration to use to create an OutputItem
     * @return An OutputItem to use
     */
    public static OutputItem create(Configuration config) {
        return switch (config.getOutputType()) {
            case "cmd", "CMD" -> new CmdOutputItem(config);
            case "udp", "UDP" -> new UDPOutputItem(config);
            case "tcp", "TCP" -> new TCPOutputItem(config);
            case "file", "FILE" -> new FileOutputItem(config);
            case "kafka", "KAFKA" -> new KafkaOutputItem(config);
            default -> throw new RuntimeException("Illegal output type: " + config.getOutputType());
        };
    }
}
