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
     * @param config The command line arguments
     */
    public KafkaOutputItem(Configuration config) {
        super(config);
        super.addListener(this);
        this.clientId = config.getValue("-ocn");
        this.topicName = config.getValue("-otn");
        this.bootstrapServer = config.getValue("-obs");

        if (null == clientId) {
            throw new RuntimeException(config.getNotFoundInformation("-ocn"));
        }

        if (null == topicName) {
            throw new RuntimeException(config.getNotFoundInformation("-otn"));
        }

        if (null == bootstrapServer) {
            throw new RuntimeException(config.getNotFoundInformation("-obs"));
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
