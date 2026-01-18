package com.kong.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kong.kafka.consumer.KafkaConsumerClient;
import com.kong.kafka.consumer.KafkaConsumerConfig;
import com.kong.kafka.factory.KafkaConsumerClientFactory;
import com.kong.schemas.avro.v1.UserEvent;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Sample Consumer Application demonstrating how to: 1. Consume messages from Kafka 2. Process
 * messages with automatic retry 3. Log and process data (can be extended to send to any external
 * system)
 */
@Slf4j
public class SampleConsumerApp {

  private static final String BOOTSTRAP_SERVERS = "localhost:9092";
  private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
  private static final String TOPIC = "user-events";
  private static final String GROUP_ID = "user-event-processor";
  private static ObjectMapper mapper = new ObjectMapper();

  public SampleConsumerApp() {
    // Simple constructor - no external dependencies needed
  }

  public static void main(String[] args) {
    log.info("Starting Sample Consumer Application");

    SampleConsumerApp app = new SampleConsumerApp();
    app.startConsuming();
  }

  public void startConsuming() {

    // Create consumer configuration
    KafkaConsumerConfig config =
        KafkaConsumerConfig.builder()
            .bootstrapServers(BOOTSTRAP_SERVERS) //  Connect here
            .groupId(GROUP_ID) // Join this consumer group
            .schemaRegistryUrl(SCHEMA_REGISTRY_URL) // For Avro schemas
            .enableAutoCommit(false) // Manual commit (safer)
            .autoOffsetReset(
                "earliest") // Read from beginning latest to read from end of the stream
            .maxPollRecords(500) // Get 500 records per poll
            .maxPollIntervalMs(300000) // Poll every 5 min max
            .sessionTimeoutMs(30000) // 30s without heartbeat = dead
            .heartbeatIntervalMs(3000) // Send heartbeat every 3s
            .build();

    // Behavior:
    // Polls up to 500 records every few seconds
    // Manually commits offsets after processing
    // Sends heartbeat every 3 seconds
    // Must poll within 5 minutes or get kicked out
    // Reads all historical data on first run

    log.info("Consumer configuration: {}", config);

    // Create consumer with message processor
    KafkaConsumerClient<UserEvent> consumer =
        KafkaConsumerClientFactory.createAvroConsumer(
            config,
            record -> {
              try {
                UserEvent event = record.value();
                log.info(
                    "Processing event: userId={}, eventType={}, partition={}, offset={}",
                    event.getUserId(),
                    event.getEventType(),
                    record.partition(),
                    record.offset());

                // Process the message
                processEvent(event);
              } catch (Exception e) {
                log.error("Failed to process event", e);
                throw new RuntimeException("Message processing failed", e);
              }
            });

    // Add shutdown hook for graceful shutdown
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("Shutdown signal received");
                  consumer.shutdown();
                }));

    // Start consuming (this blocks until shutdown)
    log.info("Starting to consume from topic: {}", TOPIC);
    consumer.subscribe(TOPIC);
  }

  /**
   * Process user event (just log it - can be extended to send to external systems). In a real
   * scenario, you would send to your external API/database.
   */
  private void processEvent(UserEvent event) {
    try {
      // Transform Avro to JSON
      String jsonData = avroToJson(event);

      // Log the received event
      log.info(
          " Processed event - User: {}, Type: {}, Email: {}",
          event.getUserId(),
          event.getEventType(),
          event.getEmail());
      log.debug("JSON data: {}", jsonData);

      // In a real application, you would send this to:
      // - Database (PostgreSQL, MySQL, etc.)
      // - Search engine (Elasticsearch, Solr, etc.)
      // - Another API
      // - Data warehouse

      // Simulate processing time
      Thread.sleep(5);

      log.info("Event processed successfully");
    } catch (Exception e) {
      log.error("Failed to process event", e);
      throw new RuntimeException("Event processing failed", e);
    }
  }

  /** Convert Avro object to JSON string. */
  private String avroToJson(UserEvent event) throws Exception {
    Map<String, Object> json = new java.util.HashMap<>();
    json.put("userId", event.getUserId().toString());
    json.put("eventType", event.getEventType().toString());
    json.put("timestamp", event.getTimestamp());
    json.put("email", event.getEmail() != null ? event.getEmail().toString() : null);
    json.put("metadata", event.getMetadata());
    return mapper.writeValueAsString(json);
  }
}
