package com.example.api.corejavaproject.kafka;

import com.example.api.corejavaproject.model.KafkaUser;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * Kafka Producer for KafkaUser messages - pure Java SE (no Spring/Quarkus)
 *
 * Usage:
 *   KafkaUserProducer producer = new KafkaUserProducer("localhost:9092");
 *   producer.send(new KafkaUser(1, "john", "123 Main St"));
 *   producer.close();
 */
public class KafkaUserProducer {
    private final KafkaProducer<String, String> producer;
    private final String topic;

    public KafkaUserProducer(String bootstrapServers, String topic) {
        this.topic = topic;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        this.producer = new KafkaProducer<>(props);
    }

    /**
     * Send a KafkaUser message to the topic
     */
    public void send(KafkaUser user) {
        String json = user.toJson();
        String key = String.valueOf(user.getId());
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, json);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                System.err.println("❌ Failed to send message: " + exception.getMessage());
                exception.printStackTrace();
            } else {
                System.out.println("✅ Sent message: " + json +
                        " to partition " + metadata.partition() +
                        " at offset " + metadata.offset());
            }
        });
    }

    /**
     * Send a KafkaUser message synchronously (blocking)
     */
    public void sendSync(KafkaUser user) throws Exception {
        String json = user.toJson();
        String key = String.valueOf(user.getId());
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, json);

        producer.send(record).get(); // blocking call
        System.out.println("✅ Sent message (sync): " + json);
    }

    /**
     * Close the producer
     */
    public void close() {
        producer.flush();
        producer.close();
        System.out.println("🔴 Kafka producer closed");
    }

    public static void main(String[] args) {
        String bootstrapServers = "localhost:9092";
        String topic = "kafka-user-topic";

        if (args.length >= 1) {
            bootstrapServers = args[0];
        }
        if (args.length >= 2) {
            topic = args[1];
        }

        System.out.println("=".repeat(60));
        System.out.println("  Kafka User Producer");
        System.out.println("=".repeat(60));
        System.out.println("Bootstrap servers: " + bootstrapServers);
        System.out.println("Topic: " + topic);
        System.out.println("-".repeat(60));

        KafkaUserProducer producer = new KafkaUserProducer(bootstrapServers, topic);

        try {
            // Send sample users
            producer.send(new KafkaUser(1, "john_doe", "123 Main Street, NYC"));
            producer.send(new KafkaUser(2, "jane_smith", "456 Oak Avenue, LA"));
            producer.send(new KafkaUser(3, "bob_wilson", "789 Pine Road, Chicago"));

            // Give time for async sends to complete
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } finally {
            producer.close();
        }
    }
}