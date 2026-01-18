package com.kong.sample;

import com.kong.kafka.factory.KafkaProducerClientFactory;
import com.kong.kafka.producer.KafkaProducerClient;
import com.kong.kafka.producer.KafkaProducerConfig;
import com.kong.kafka.schema.SchemaRegistryProvider;
import com.kong.schemas.avro.v1.EventType;
import com.kong.schemas.avro.v1.UserEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * Sample Producer Application demonstrating how to: 1. Define and register a schema 2. Use the
 * kafka-common-lib to produce messages
 */
@Slf4j
public class SampleProducerApp {

  private static final String BOOTSTRAP_SERVERS = "localhost:9092";
  private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
  private static final String TOPIC = "user-events";

  public static void main(String[] args) throws IOException {
    log.info("Starting Sample Producer Application");

    // Step 1: Register schema with Schema Registry
    registerSchema();

    // Step 2: Create producer configuration
    // Your configuration creates this behavior:
    KafkaProducerConfig config =
        KafkaProducerConfig.builder()
            .bootstrapServers(BOOTSTRAP_SERVERS)
            .schemaRegistryUrl(SCHEMA_REGISTRY_URL)
            .enableIdempotence(true) //  No duplicates
            .acks("all") // Wait for all replicas
            .retries(5) // Retry up to 5 times (handled by Kafka)
            .compressionType("snappy") //  Compress ~50%
            .lingerMs(10) //  Batch for 10ms
            .batchSize(32768) // Up to 32 KB per batch
            .requestTimeoutMs(30000) // 30s per request
            .deliveryTimeoutMs(120000) // 2 min total
            .build();

    // Result:
    //  Highly reliable (no data loss)
    //  Good throughput (batching + compression)
    //  ~50% bandwidth savings
    //  ~10ms latency per message

    log.info("Producer configuration: {}", config);

    // Step 3: Create producer client using factory
    try (KafkaProducerClient<UserEvent> producer =
        KafkaProducerClientFactory.createAvroProducer(config)) {

      // Step 4: Produce sample messages
      produceSampleMessages(producer);

      log.info("All messages sent successfully!");

    } catch (Exception e) {
      log.error("Failed to produce messages", e);
    }
  }

  private static void registerSchema() throws IOException {
    log.info("Registering schema with Schema Registry");

    SchemaRegistryProvider schemaRegistryProvider = new SchemaRegistryProvider(SCHEMA_REGISTRY_URL);

    // Load schema from kafka-schemas module (via classpath)
    // The schema is packaged in kafka-schemas JAR at /avro/v1/user-event-v1.avsc
    var schemaStream = SampleProducerApp.class.getResourceAsStream("/avro/v1/user-event-v1.avsc");

    if (schemaStream == null) {
      throw new IOException(
          "Schema file not found in kafka-schemas module. Make sure kafka-schemas dependency is"
              + " included and contains /avro/v1/user-event-v1.avsc");
    }

    Schema schema = new Schema.Parser().parse(schemaStream);

    // Register schema for topic value
    String subject = TOPIC + "-value";
    int schemaId = schemaRegistryProvider.registerSchema(subject, schema);

    log.info("Schema registered with ID: {} for subject: {}", schemaId, subject);
  }

  private static void produceSampleMessages(KafkaProducerClient<UserEvent> producer) {
    // Create sample user events
    UserEvent[] events = {
      createUserEvent("user-001", EventType.CREATED, "alice@example.com", "ip", "192.168.1.1"),
      createUserEvent("user-002", EventType.LOGIN, "bob@example.com", "device", "mobile"),
      createUserEvent(
          "user-003", EventType.UPDATED, "charlie@example.com", "action", "profile_update"),
      createUserEvent("user-001", EventType.LOGOUT, "alice@example.com", "duration", "3600"),
      createUserEvent("user-004", EventType.DELETED, null, "reason", "account_closure")
    };

    // Send messages
    for (UserEvent event : events) {
      try {
        // Synchronous send with automatic retry
        RecordMetadata metadata = producer.sendSync(TOPIC, event.getUserId().toString(), event);

        log.info(
            "Sent event: userId={}, eventType={}, partition={}, offset={}",
            event.getUserId(),
            event.getEventType(),
            metadata.partition(),
            metadata.offset());
      } catch (Exception e) {
        log.error("Failed to send event for user: {}", event.getUserId(), e);
      }
    }
  }

  private static UserEvent createUserEvent(
      String userId, EventType eventType, String email, String metaKey, String metaValue) {
    Map<String, String> metadata = new HashMap<>();
    metadata.put(metaKey, metaValue);
    metadata.put("source", "sample-app");

    return UserEvent.newBuilder()
        .setUserId(userId)
        .setEventType(eventType)
        .setTimestamp(System.currentTimeMillis())
        .setEmail(email)
        .setMetadata(metadata)
        .build();
  }
}
