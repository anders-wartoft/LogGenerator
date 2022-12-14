package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.util.CommandLineParser;
import org.apache.kafka.clients.consumer.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


public class KafkaInputItem extends AbstractInputItem {
    static final Logger logger = Logger.getLogger(KafkaInputItem.class.getName());
    /** The hostname:port to connect to */
    private final String bootstrapServer;
    /** The client id to use */
    private final String clientId;

    /** Connection properties */
    private final Properties properties = new Properties();

    /** The Kafka consumer */
    private KafkaConsumer<Integer, String> consumer;

    /** The topic name */
    private final String topicName;

    /**
     * Create a new KafkaInputItem
     * @param args The command line arguments
     */
    public KafkaInputItem(String [] args) {
        super(args);
        this.clientId = CommandLineParser.getCommandLineArgument(args, "cn", "client-name", "The Client ID to use in Kafka input and output items");
        this.topicName = CommandLineParser.getCommandLineArgument(args, "tn", "topic-name", "The Topic Name to use in Kafka input and output items");
        this.bootstrapServer = CommandLineParser.getCommandLineArgument(args, "bs", "bootstrap-server", "The address (host:port) to Kafka input and output items");

        if (null == clientId) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("client-name is required in Kafka");
        }

        if (null == topicName) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("topic-name is required in Kafka");
        }

        if (null == bootstrapServer) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("bootstrap-server is required in Kafka");
        }
    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, clientId);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.IntegerDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(properties);
        logger.info("Connected to kafka " + this.bootstrapServer);
        consumer.subscribe(Collections.singletonList(this.topicName));
        logger.info("Subscribed to topic " + this.topicName);
    }

    /**
     * Are there more messages to read?
     * We'll just wait if there are no messages now...
     * @return True iff there are more messages
     */
    public boolean hasNext() {
        return true;
    }

    /**
     * Read input one line at a time and return
     * {batchSize} elements.
     * @return The read input
     */
    public List<String> next() {
        ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofMillis(1000));
        List<String> result = new ArrayList<>();
        for (ConsumerRecord<Integer, String> record: records) {
            result.add(record.value());
        }
        logger.log(Level.FINEST, result.toString());
        return result;
    }

    /**
     * Let the item teardown after reading.
     * Will be called after Ctrl-C
     */
    public void teardown() {
        // Ignore. consumer.close will loop here
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "KafkaInputItem" + System.lineSeparator() +
                bootstrapServer + System.lineSeparator() +
                clientId + System.lineSeparator() +
                topicName;
    }
}
