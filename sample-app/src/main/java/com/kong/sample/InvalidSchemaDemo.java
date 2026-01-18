package com.kong.sample;

import com.kong.kafka.factory.KafkaProducerClientFactory;
import com.kong.kafka.producer.KafkaProducerClient;
import com.kong.kafka.producer.KafkaProducerConfig;
import com.kong.schemas.avro.v1.EventType;
import com.kong.schemas.avro.v1.UserEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

/**
 * Demo class to show what happens when you send data with invalid schema.
 *
 * <p>Run this to see schema validation errors in action!
 */
@Slf4j
public class InvalidSchemaDemo {

  private static final String BOOTSTRAP_SERVERS = "localhost:9092";
  private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
  private static final String TOPIC = "user-events";

  public static void main(String[] args) {

    // Example 1: Missing required field
    log.info(" Example 1: Missing Required Field ---");
    tryMissingRequiredField();

    // Example 2: Wrong field type (using GenericRecord)
    log.info("Example 2: Wrong Field Type ");
    tryWrongFieldType();

    // Example 3: Valid schema (for comparison)
    log.info(" Valid Schema (Success)");
    tryValidSchema();
  }

  /** Example 1: Avro won't even let you create a record without required fields */
  private static void tryMissingRequiredField() {
    try {
      // This will fail at compile time because Avro builder enforces required fields
      UserEvent event =
          UserEvent.newBuilder()
              // Missing required field: userId
              .setEventType(EventType.CREATED)
              .setTimestamp(System.currentTimeMillis())
              .build();

      log.info("Created event (this won't compile without userId)");

    } catch (Exception e) {
      log.error("Failed to create event: {}", e.getMessage());
    }
  }

  /**
   * Example 2: Wrong field type using GenericRecord This bypasses compile-time checks but fails at
   * serialization
   */
  private static void tryWrongFieldType() {
    try {
      KafkaProducerConfig config =
          KafkaProducerConfig.builder()
              .bootstrapServers(BOOTSTRAP_SERVERS)
              .schemaRegistryUrl(SCHEMA_REGISTRY_URL)
              .build();

      // Create producer for GenericRecord
      KafkaProducerClient<GenericRecord> producer =
          KafkaProducerClientFactory.createAvroProducer(config);

      // Load the UserEvent schema
      Schema schema = UserEvent.getClassSchema();

      // Create a record with WRONG data type for timestamp (String instead of Long)
      GenericRecord invalidRecord = new GenericData.Record(schema);
      invalidRecord.put("userId", "user-001");
      invalidRecord.put("eventType", "CREATED");
      invalidRecord.put("timestamp", "this-should-be-a-long-not-a-string"); // ❌ Wrong type!
      invalidRecord.put("email", null);
      invalidRecord.put("metadata", null);

      log.info("Attempting to send record with wrong type...");
      producer.sendSync(TOPIC, "key-1", invalidRecord);

      log.info(" Message sent (this shouldn't happen!)");
      producer.close();

    } catch (Exception e) {
      log.error("Schema validation failed (expected): {}", e.getMessage());
      log.error(
          "Root cause: {}", e.getCause() != null ? e.getCause().getClass().getSimpleName() : "N/A");
    }
  }

  /** Example 3: Valid schema - this works! */
  private static void tryValidSchema() {
    try {
      KafkaProducerConfig config =
          KafkaProducerConfig.builder()
              .bootstrapServers(BOOTSTRAP_SERVERS)
              .schemaRegistryUrl(SCHEMA_REGISTRY_URL)
              .build();

      KafkaProducerClient<UserEvent> producer =
          KafkaProducerClientFactory.createAvroProducer(config);

      // Create a VALID event with all required fields
      UserEvent validEvent =
          UserEvent.newBuilder()
              .setUserId("user-001")
              .setEventType(EventType.CREATED)
              .setTimestamp(System.currentTimeMillis())
              .setEmail("alice@example.com")
              .setMetadata(null)
              .build();

      log.info("Attempting to send valid record...");
      producer.sendSync(TOPIC, "key-1", validEvent);

      log.info("Message sent successfully!");
      producer.close();

    } catch (Exception e) {
      log.error("Unexpected failure: {}", e.getMessage());
    }
  }
}
