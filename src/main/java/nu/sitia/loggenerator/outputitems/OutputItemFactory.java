package nu.sitia.loggenerator.outputitems;

import nu.sitia.loggenerator.util.CommandLineParser;


public class OutputItemFactory {

    /**
     * Create an OutputItem depending on the configuration
     * @param args The command line arguments to use to create an OutputItem
     * @return An OutputItem to use
     */
    public static OutputItem create(String [] args) {
        String outputType = CommandLineParser.getCommandLineArgument(args, "o", "output", "Output module name (cmd, udp, tcp, kafka, file or none)");
        if (null == outputType) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("Parameter -o (--output) is required");
        }

        return switch (outputType) {
            case "cmd", "CMD" -> new CmdOutputItem(args);
            case "udp", "UDP" -> new UDPOutputItem(args);
            case "tcp", "TCP" -> new TCPOutputItem(args);
            case "file", "FILE" -> new FileOutputItem(args);
            case "kafka", "KAFKA" -> new KafkaOutputItem(args);
            case "null", "NULL" -> new NullOutputItem(args);
            default -> throw new RuntimeException("Illegal output type: " + outputType);
        };
    }
}
