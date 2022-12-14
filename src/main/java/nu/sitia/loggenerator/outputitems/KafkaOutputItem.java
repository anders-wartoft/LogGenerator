package nu.sitia.loggenerator.outputitems;


import nu.sitia.loggenerator.util.CommandLineParser;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class KafkaOutputItem extends AbstractOutputItem implements SendListener {
    static final Logger logger = Logger.getLogger(KafkaOutputItem.class.getName());
    /** The hostname:port to connect to */
    private final String bootstrapServer;
    /** The client id to use */
    private final String clientId;

    /** Connection properties */
    private final Properties properties = new Properties();

    /** The Kafka consumer */
    private KafkaProducer<Integer, String> producer;

    /** The topic name */
    private final String topicName;

    /** The Kafka message number */
    private int messageNr = 1;

    /**
     * Constructor. Add the callback method from this class.
     * @param args The command line arguments
     */
    public KafkaOutputItem(String [] args) {
        super(args);
        super.addListener(this);
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
     * Write to Kafka
     * @param elements The element to write
     * @throws RuntimeException Not thrown here
     */
    @Override
    public void write(List<String> elements) throws RuntimeException {
        super.write(elements);
    }

    /**
     * Callback. What to do when the cache is full.
     * Writes to hostname:port
     * @param toSend String to send
     */
    @Override
    public void send(List<String> toSend) {
        logger.info("Sending " + toSend.size() + " messages to " + topicName);
        if (toSend.size() > 0) {
            try {
                String message = String.join("\n", toSend);
                logger.finest("Sending: " + message);
                producer.send(new ProducerRecord<>(
                        topicName,
                        messageNr++,
                        message)).get();
                logger.finer("Sent message without exception");
            } catch (InterruptedException e) {
                // ignore (terminate)
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setup() throws RuntimeException {
        super.setup();
        properties.put("bootstrap.servers", bootstrapServer);
        properties.put("client.id", clientId);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(properties);
        logger.info("Connected to kafka " + this.bootstrapServer);
        addTransactionMessages = true;
    }

    @Override
    public void teardown() {
        super.teardown();
        producer.close();
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "KafkaOutputItem" + System.lineSeparator() +
                bootstrapServer + System.lineSeparator() +
                clientId + System.lineSeparator() +
                topicName;
    }
}
