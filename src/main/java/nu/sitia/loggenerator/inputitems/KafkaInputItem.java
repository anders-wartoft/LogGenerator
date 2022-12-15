/*
 * Copyright 2022 sitia.nu https://github.com/anders-wartoft/LogGenerator
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.Configuration;
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
     * @param config The command line arguments
     */
    public KafkaInputItem(Configuration config) {
        super(config);
        this.clientId = config.getValue("-icn");
        this.topicName = config.getValue("-itn");
        this.bootstrapServer = config.getValue("-ibs");

        if (null == clientId) {
            throw new RuntimeException(config.getNotFoundInformation("-icn"));
        }

        if (null == topicName) {
            throw new RuntimeException(config.getNotFoundInformation("-itn"));
        }

        if (null == bootstrapServer) {
            throw new RuntimeException(config.getNotFoundInformation("-ibs"));
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
