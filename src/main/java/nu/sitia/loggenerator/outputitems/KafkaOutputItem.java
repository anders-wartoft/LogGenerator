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

package nu.sitia.loggenerator.outputitems;


import nu.sitia.loggenerator.Configuration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class KafkaOutputItem extends AbstractOutputItem implements SendListener {
    static final Logger logger = Logger.getLogger(KafkaOutputItem.class.getName());
    /** The hostname:port to connect to */
    private String bootstrapServer;
    /** The client id to use */
    private String clientId;

    /** Connection properties */
    private final Properties properties = new Properties();

    /** The Kafka consumer */
    private KafkaProducer<Integer, String> producer;

    /** The topic name */
    private String topicName;

    /** The Kafka message number */
    private int messageNr = 1;

    /**
     * Constructor. Add the callback method from this class.
     * @param config The command line arguments
     */
    public KafkaOutputItem(Configuration config) {
        super(config);
        super.addListener(this);
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("KafkaOutputItem. Write to Kafka\n" +
                    "Parameters:\n" +
                    "--client-id, -ci <client id> The client id to use\n" +
                    "--topic, -t <topic name> The topic name to write to\n" +
                    "--bootstrap-server, -b <hostname:port> The hostname:port to connect to\n");
            System.exit(1);
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
        logger.fine("Sending " + toSend.size() + " messages to " + topicName);
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
