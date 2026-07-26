package com.example.api.corejavaproject.kafka;

import com.example.api.corejavaproject.model.KafkaUser;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * Kafka Consumer for KafkaUser messages - pure Java SE (no Spring/Quarkus)
 *
 * Usage:
 *   KafkaUserConsumer consumer = new KafkaUserConsumer("localhost:9092", "kafka-user-topic", "consumer-group-1");
 *   consumer.consume(); // runs continuously
 *   consumer.close();
 */
public class KafkaUserConsumer {
    private final KafkaConsumer<String, String> consumer;
    private final String topic;
    private volatile boolean running = true;

    public KafkaUserConsumer(String bootstrapServers, String topic, String groupId) {
        this.topic = topic;

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Collections.singletonList(topic));
    }

    /**
     * Consume messages continuously until stop() is called
     */
    public void consume() {
        System.out.println("📥 Starting to consume messages from topic: " + topic);
        System.out.println("-".repeat(60));

        try {
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        KafkaUser user = KafkaUser.fromJson(record.value());
                        System.out.println("📩 Received message:");
                        System.out.println("  Topic: " + record.topic());
                        System.out.println("  Partition: " + record.partition());
                        System.out.println("  Offset: " + record.offset());
                        System.out.println("  Key: " + record.key());
                        System.out.println("  User: " + user);
                    } catch (Exception e) {
                        System.err.println("❌ Failed to parse message: " + e.getMessage());
                        System.out.println("  Raw value: " + record.value());
                    }
                }
            }
        } finally {
            consumer.close();
            System.out.println("🔴 Kafka consumer closed");
        }
    }

    /**
     * Consume a single batch of messages (for testing)
     */
    public void consumeOnce(int maxMessages, int timeoutMs) {
        System.out.println("📥 Consuming up to " + maxMessages + " messages from topic: " + topic);
        System.out.println("-".repeat(60));

        int count = 0;
        try {
            while (count < maxMessages) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(timeoutMs));

                if (records.isEmpty()) {
                    break;
                }

                for (ConsumerRecord<String, String> record : records) {
                    if (count >= maxMessages) break;

                    try {
                        KafkaUser user = KafkaUser.fromJson(record.value());
                        System.out.println("📩 [" + (count + 1) + "] " + user);
                        count++;
                    } catch (Exception e) {
                        System.err.println("❌ Failed to parse: " + e.getMessage());
                    }
                }
            }
        } finally {
            consumer.close();
            System.out.println("-".repeat(60));
            System.out.println("📊 Total messages received: " + count);
        }
    }

    /**
     * Stop consuming messages
     */
    public void stop() {
        running = false;
        System.out.println("🛑 Consumer stop requested...");
    }

    public static void main(String[] args) {
        String bootstrapServers = "localhost:9092";
        String topic = "kafka-user-topic";
        String groupId = "kafka-user-consumer-group";

        if (args.length >= 1) {
            bootstrapServers = args[0];
        }
        if (args.length >= 2) {
            topic = args[1];
        }
        if (args.length >= 3) {
            groupId = args[2];
        }

        System.out.println("=".repeat(60));
        System.out.println("  Kafka User Consumer");
        System.out.println("=".repeat(60));
        System.out.println("Bootstrap servers: " + bootstrapServers);
        System.out.println("Topic: " + topic);
        System.out.println("Group ID: " + groupId);
        System.out.println("-".repeat(60));

        KafkaUserConsumer consumer = new KafkaUserConsumer(bootstrapServers, topic, groupId);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutdown hook triggered...");
            consumer.stop();
        }));

        consumer.consume();
    }
}