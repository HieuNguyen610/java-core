package com.example.api.corejavaproject.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * ORC to Kafka Producer - reads data from ORC datalake files and sends to Kafka.
 * Pure Java SE implementation (no Spring/Quarkus).
 *
 * Usage:
 *   OrcKafkaProducer producer = new OrcKafkaProducer("localhost:9092", "my-topic");
 *   producer.sendOrcFile("/path/to/data.orc");
 *   producer.close();
 *
 * Or with custom key column:
 *   OrcKafkaProducer producer = new OrcKafkaProducer("localhost:9092", "my-topic", "id");
 *   producer.sendOrcFile("/path/to/data.orc");
 *   producer.close();
 */
public class OrcKafkaProducer implements Closeable {
    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final String keyColumn;
    private final OrcReader orcReader;
    private long messagesSent;
    private long errors;

    /**
     * Create an ORC to Kafka producer
     *
     * @param bootstrapServers Kafka bootstrap servers (e.g., "localhost:9092")
     * @param topic Kafka topic to send messages to
     */
    public OrcKafkaProducer(String bootstrapServers, String topic) {
        this(bootstrapServers, topic, null);
    }

    /**
     * Create an ORC to Kafka producer with a key column
     *
     * @param bootstrapServers Kafka bootstrap servers (e.g., "localhost:9092")
     * @param topic Kafka topic to send messages to
     * @param keyColumn Column name to use as Kafka message key (null for random keys)
     */
    public OrcKafkaProducer(String bootstrapServers, String topic, String keyColumn) {
        this.topic = topic;
        this.keyColumn = keyColumn;
        this.orcReader = null;
        this.messagesSent = 0;
        this.errors = 0;

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
     * Send all rows from an ORC file to Kafka
     *
     * @param orcFilePath Path to the ORC file
     */
    public void sendOrcFile(String orcFilePath) throws IOException {
        sendOrcFile(orcFilePath, -1, null);
    }

    /**
     * Send rows from an ORC file to Kafka with filtering options
     *
     * @param orcFilePath Path to the ORC file
     * @param maxMessages Maximum number of messages to send (-1 for all)
     * @param columnsToInclude Column names to include (null for all columns)
     */
    public void sendOrcFile(String orcFilePath, long maxMessages, List<String> columnsToInclude) throws IOException {
        System.out.println("📖 Reading ORC file: " + orcFilePath);

        try (OrcReader reader = new OrcReader(orcFilePath)) {
            System.out.println("   Schema: " + reader.getSchema());
            System.out.println("   Total rows in file: " + reader.getNumberOfRows());
            System.out.println("   Topic: " + topic);
            System.out.println("-".repeat(60));

            long rowsSent = 0;
            Set<String> includeSet = (columnsToInclude != null) ? new HashSet<>(columnsToInclude) : null;

            while (reader.hasNext()) {
                if (maxMessages > 0 && rowsSent >= maxMessages) {
                    System.out.println("   Reached max messages limit: " + maxMessages);
                    break;
                }

                Map<String, Object> row = reader.next();
                Map<String, Object> filteredRow = (includeSet != null) ? filterColumns(row, includeSet) : row;

                String json = reader.toJson(filteredRow);
                String key = (keyColumn != null && row.containsKey(keyColumn))
                    ? String.valueOf(row.get(keyColumn))
                    : UUID.randomUUID().toString();

                sendMessage(key, json);
                rowsSent++;

                if (rowsSent % 1000 == 0) {
                    System.out.println("   Progress: " + rowsSent + " messages sent...");
                }
            }

            System.out.println("-".repeat(60));
            System.out.println("✅ ORC file processing completed");
            System.out.println("   Total rows read: " + reader.getRowsRead());
            System.out.println("   Messages sent: " + messagesSent);
            System.out.println("   Errors: " + errors);
        }
    }

    /**
     * Send rows from an ORC file synchronously (blocking)
     *
     * @param orcFilePath Path to the ORC file
     */
    public void sendOrcFileSync(String orcFilePath) throws Exception {
        sendOrcFileSync(orcFilePath, -1, null);
    }

    /**
     * Send rows from an ORC file synchronously with options
     *
     * @param orcFilePath Path to the ORC file
     * @param maxMessages Maximum number of messages to send (-1 for all)
     * @param columnsToInclude Column names to include (null for all columns)
     */
    public void sendOrcFileSync(String orcFilePath, long maxMessages, List<String> columnsToInclude) throws Exception {
        System.out.println("📖 Reading ORC file (sync): " + orcFilePath);

        try (OrcReader reader = new OrcReader(orcFilePath)) {
            System.out.println("   Schema: " + reader.getSchema());
            System.out.println("   Total rows in file: " + reader.getNumberOfRows());
            System.out.println("-".repeat(60));

            long rowsSent = 0;
            Set<String> includeSet = (columnsToInclude != null) ? new HashSet<>(columnsToInclude) : null;

            while (reader.hasNext()) {
                if (maxMessages > 0 && rowsSent >= maxMessages) {
                    break;
                }

                Map<String, Object> row = reader.next();
                Map<String, Object> filteredRow = (includeSet != null) ? filterColumns(row, includeSet) : row;

                String json = reader.toJson(filteredRow);
                String key = (keyColumn != null && row.containsKey(keyColumn))
                    ? String.valueOf(row.get(keyColumn))
                    : UUID.randomUUID().toString();

                sendMessageSync(key, json);
                rowsSent++;

                if (rowsSent % 1000 == 0) {
                    System.out.println("   Progress: " + rowsSent + " messages sent...");
                }
            }

            System.out.println("-".repeat(60));
            System.out.println("✅ ORC file processing completed (sync)");
            System.out.println("   Total rows read: " + reader.getRowsRead());
            System.out.println("   Messages sent: " + messagesSent);
            System.out.println("   Errors: " + errors);
        }
    }

    /**
     * Filter columns from a row
     */
    private Map<String, Object> filterColumns(Map<String, Object> row, Set<String> columnsToInclude) {
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (columnsToInclude.contains(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    /**
     * Send a single message asynchronously
     */
    private void sendMessage(String key, String value) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

        try {
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("❌ Failed to send message: " + exception.getMessage());
                    errors++;
                } else {
                    messagesSent++;
                }
            });
        } catch (Exception e) {
            System.err.println("❌ Error sending message: " + e.getMessage());
            errors++;
        }
    }

    /**
     * Send a single message synchronously (blocking)
     */
    private void sendMessageSync(String key, String value) throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record).get();
        messagesSent++;
    }

    /**
     * Flush and close the producer
     */
    @Override
    public void close() {
        producer.flush();
        producer.close();
        System.out.println("🔴 ORC to Kafka producer closed");
    }

    /**
     * Get the number of messages successfully sent
     */
    public long getMessagesSent() {
        return messagesSent;
    }

    /**
     * Get the number of errors
     */
    public long getErrors() {
        return errors;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java OrcKafkaProducer <orc-file-path> <kafka-bootstrap-servers> [topic] [key-column]");
            System.out.println();
            System.out.println("Example - send ORC file to Kafka:");
            System.out.println("  java OrcKafkaProducer /path/to/data.orc localhost:9092 my-topic");
            System.out.println();
            System.out.println("Example - with key column:");
            System.out.println("  java OrcKafkaProducer /path/to/data.orc localhost:9092 my-topic id");
            System.out.println();
            System.out.println("Example - read specific columns only:");
            System.out.println("  Edit code to use columnsToInclude parameter in sendOrcFile()");
            return;
        }

        String orcFilePath = args[0];
        String bootstrapServers = args[1];
        String topic = args.length >= 3 ? args[2] : "orc-datalake-topic";
        String keyColumn = args.length >= 4 ? args[3] : null;

        System.out.println("=".repeat(60));
        System.out.println("  ORC to Kafka Producer");
        System.out.println("=".repeat(60));
        System.out.println("ORC File: " + orcFilePath);
        System.out.println("Bootstrap servers: " + bootstrapServers);
        System.out.println("Topic: " + topic);
        System.out.println("Key column: " + (keyColumn != null ? keyColumn : "(random)"));
        System.out.println("-".repeat(60));

        try (OrcKafkaProducer producer = new OrcKafkaProducer(bootstrapServers, topic, keyColumn)) {
            producer.sendOrcFile(orcFilePath);

            // Give time for async sends to complete
            Thread.sleep(2000);

            System.out.println("-".repeat(60));
            System.out.println("📊 Final Statistics:");
            System.out.println("   Messages sent: " + producer.getMessagesSent());
            System.out.println("   Errors: " + producer.getErrors());

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}