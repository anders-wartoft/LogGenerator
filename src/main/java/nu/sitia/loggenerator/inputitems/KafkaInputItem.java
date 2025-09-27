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
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.Configuration;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.UnknownTopicIdException;

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
    private String bootstrapServer;
    /** The client id to use */
    private String clientId;

    /** Connection properties */
    private final Properties properties = new Properties();

    /** The Kafka consumer */
    private KafkaConsumer<Integer, String> consumer;

    /** The topic name */
    private String topicName;

    /** Whether to print the keys read from Kafka */
    private boolean printKeys = false;

    /** The default Kafka batch size */
    private final static int DEFAULT_KAFKA_BATCHSIZE = 200;

    /**
     * Create a new KafkaInputItem
     */
    public KafkaInputItem(Configuration config) {
        super(config);
        this.batchSize = DEFAULT_KAFKA_BATCHSIZE;
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("KafkaInputItem. Read from a Kafka topic\n" +
                    "Parameters:\n" +
                    "--client-id <client-id> (-ci <client-id>)\n" +
                    "  The client id to use\n" +
                    "--topic <topic> (-t <topic>)\n" +
                    "  The topic to read from\n" +
                    "--bootstrap-server <hostname:port> (-b <hostname:port>)\n" +
                    "  The hostname:port to connect to\n" +
                    "--print-keys <true|false> (-pk <true|false>)\n" +
                    "  Whether to print the keys read from Kafka. Default false\n");
            super.setParameter(key, value);
        }
        if (super.setParameter(key, value)) {
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--client-id") || key.equalsIgnoreCase("-ci"))) {
            this.clientId = value;
            logger.fine("clientId " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--topic") || key.equalsIgnoreCase("-t"))) {
            this.topicName = value;
            logger.fine("topic " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--bootstrap-server") || key.equalsIgnoreCase("-b"))) {
            this.bootstrapServer = value;
            logger.fine("bootstrapServer " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--print-keys") || key.equalsIgnoreCase("-pk"))) {
            this.printKeys = Boolean.parseBoolean(value);
            logger.fine("printKeys " + value);
            return true;
        }
        return false;
    }

    @Override
    public boolean afterPropertiesSet() {
        if (null == this.clientId) {
            throw new RuntimeException("Missing --client-id");
        }
        if (null == this.topicName) {
            throw new RuntimeException("Missing --topic");
        }
        if (null == this.bootstrapServer) {
            throw new RuntimeException("Missing --bootstrap-server");
        }
        return true;
    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, clientId);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(batchSize));
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, "1000");

        consumer = new KafkaConsumer<>(properties);
        logger.info("Connected to kafka " + this.bootstrapServer);
        consumer.subscribe(Collections.singletonList(this.topicName));
        consumer.poll(Duration.ofSeconds(5)); // fetch metadata and leader info
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
        List<String> result = new ArrayList<>();
        while (true) {
            try {
                ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<Integer, String> record: records) {
                    if (printKeys) {
                        result.add(record.key() + ": " + record.value());
                    } else {
                        result.add(record.value());
                    }
                }
                logger.log(Level.FINEST, result.toString());
                return result;
            } catch (UnknownTopicIdException e) {
                try {
                    // wait a bit and retry
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    return result;
                }
            }
        }
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
                "Bootstrap-server: " + bootstrapServer + System.lineSeparator() +
                "Client-id: " + clientId + System.lineSeparator() +
                "Topic: " + topicName + System.lineSeparator() +
                "Batch size: " + batchSize + System.lineSeparator();
    }
}
