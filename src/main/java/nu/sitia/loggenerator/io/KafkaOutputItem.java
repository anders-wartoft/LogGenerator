package nu.sitia.loggenerator.io;


import nu.sitia.loggenerator.util.Configuration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class KafkaOutputItem extends AbstractOutputItem implements SendListener {
    static Logger logger = Logger.getLogger(KafkaOutputItem.class.getName());
    /** The hostname:port to connect to */
    private final String bootstrapServer;
    /** The client id to use */
    private final String clientId;

    /** Connection properties */
    private final Properties properties = new Properties();

    /** The Kafka consumer */
    private KafkaProducer producer;

    /** The topic name */
    private final String topicName;

    /**
     * Constructor. Add the callback method from this class.
     * @param config The Configuration object
     */
    public KafkaOutputItem(Configuration config) {
        super();
        super.addListener(this);
        bootstrapServer = config.getBootstrapServer();
        clientId = config.getClientName();
        topicName = config.getTopicName();
        if (null == clientId) {
            throw new RuntimeException("client-name is required in Kafka");
        }
        if (null == topicName) {
            throw new RuntimeException("topic-name is required in Kafka");
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
        int messageNr = 1;
        try {
            for (Iterator<String> i=toSend.iterator(); i.hasNext(); ) {
                String message = i.next();
                logger.fine("Sending: " + message);
                producer.send(new ProducerRecord<>(
                        topicName,
                        messageNr,
                        message)).get();
                messageNr++;
            }
            logger.finer("Sent message without exception");
        } catch (InterruptedException  e) {
            // ignore (terminate)
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setup() throws RuntimeException {
        super.setup();
        properties.put("bootstrap.servers", bootstrapServer);
        properties.put("client.id", clientId);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer(properties);
        logger.info("Connected to kafka " + this.bootstrapServer);
    }

    @Override
    public void teardown() {
        super.teardown();
        producer.close();
    }
}
