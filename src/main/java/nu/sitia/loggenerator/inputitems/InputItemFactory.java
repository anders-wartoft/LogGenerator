package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.util.CommandLineParser;


import java.io.File;

public class InputItemFactory {

    /**
     * Create an InputItem depending on the configuration
     * @param args The command line arguments to use to create an InputItem
     * @return An InputItem to use
     */
    public static InputItem create(String [] args) {
        String input = CommandLineParser.getCommandLineArgument(args, "i", "input", "Input module name (udp, tcp, kafka or file");
        String inputName = CommandLineParser.getCommandLineArgument(args, "in", "input-name", "Input file name");
        if (null == input) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("Parameter -i (--input) is required");
        }

        return switch (input) {
            case "template", "TEMPLATE" -> new TemplateFileInputItem(args);
            // special case. the FileInputItem needs to be able to send begin- and end messages with the filename
            case "file", "FILE" -> getFileInputItem(inputName, args);
            case "udp", "UDP" -> new UDPInputItem(args);
            case "tcp", "TCP" -> new TCPInputItem(args);
            case "kafka", "KAFKA" -> new KafkaInputItem(args);
            case "static", "STATIC" -> new StaticInputItem(args);
            case "counter", "COUNTER" -> new CounterInputItem(args);
            default -> throw new RuntimeException("Illegal input type: " + input);
        };
    }

    /**
     * Helper method to return a FileInputItem or a DirectoryInputItem
     * @param name The name of the item
     * @param args The command line arguments object
     * @return A File or Directory input item
     */
    private static InputItem getFileInputItem(String name, String [] args) {
        if (name == null) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("Parameter -in (--input-name) is required for -i file");
        }

        File file = new File(name);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + file.getAbsolutePath());
        }
        if (file.isDirectory()) {
            return new DirectoryInputItem(args);
        }
        return new FileInputItem(name, args);
    }
}
