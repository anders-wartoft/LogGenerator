package nu.sitia.loggenerator.io;

import nu.sitia.loggenerator.util.Configuration;

import java.io.File;

public class InputItemFactory {

    /**
     * Create an InputItem depending on the configuration
     * @param config The configuration to use to create an InputItem
     * @return An InputItem to use
     */
    public static InputItem create(Configuration config) {
        return switch (config.getInputType()) {
            case "template", "TEMPLATE" -> new TemplateFileInputItem(config);
            case "file", "FILE" -> getFileInputItem(config.getInputName(), config);
            case "udp", "UDP" -> new UDPInputItem(config);
            case "tcp", "TCP" -> new TCPInputItem(config);
            case "kafka", "KAFKA" -> new KafkaInputItem(config);
            default -> throw new RuntimeException("Illegal input type: " + config.getInputType());
        };
    }

    /**
     * Helper method to return a FileInputItem or a DirectoryInputItem
     * @param name The name of the item
     * @param config The configuration object
     * @return A File or Directory iput item
     */
    private static InputItem getFileInputItem(String name, Configuration config) {
        File file = new File(name);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + file.getAbsolutePath());
        }
        if (file.isDirectory()) {
            DirectoryInputItem dir = new DirectoryInputItem(config);
            return dir;
        }
        FileInputItem fi = new FileInputItem(name, config);
        return fi;
    }
}
