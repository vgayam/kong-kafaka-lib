package com.kong.kafka.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/** Configuration for Kafka Consumer with sensible defaults. */
@Getter
@Builder
@ToString
public class KafkaConsumerConfig {

  private final String bootstrapServers;

  private final String groupId;

  private final String schemaRegistryUrl;

  @Builder.Default private final boolean enableAutoCommit = false;

  @Builder.Default private final String autoOffsetReset = "earliest";

  @Builder.Default private final int maxPollRecords = 500;

  @Builder.Default private final int maxPollIntervalMs = 300000;

  @Builder.Default private final int sessionTimeoutMs = 30000;

  @Builder.Default private final int heartbeatIntervalMs = 3000;

  // Processing configuration
  @Builder.Default private final int maxRetryAttempts = 3;

  @Builder.Default private final long retryWaitDurationMs = 1000;

  @Builder.Default private final Map<String, Object> additionalProperties = new HashMap<>();

  /** Convert to Kafka Properties. */
  public Properties toProperties() {
    Properties props = new Properties();

    props.put("bootstrap.servers", bootstrapServers);
    props.put("group.id", groupId);
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("enable.auto.commit", String.valueOf(enableAutoCommit));
    props.put("auto.offset.reset", autoOffsetReset);
    props.put("max.poll.records", String.valueOf(maxPollRecords));
    props.put("max.poll.interval.ms", String.valueOf(maxPollIntervalMs));
    props.put("session.timeout.ms", String.valueOf(sessionTimeoutMs));
    props.put("heartbeat.interval.ms", String.valueOf(heartbeatIntervalMs));

    if (schemaRegistryUrl != null && !schemaRegistryUrl.isEmpty()) {
      props.put("schema.registry.url", schemaRegistryUrl);
    }

    // Add additional properties
    props.putAll(additionalProperties);

    return props;
  }

  public static class KafkaConsumerConfigBuilder {
    public KafkaConsumerConfigBuilder addProperty(String key, Object value) {
      if (this.additionalProperties$value == null) {
        this.additionalProperties$value = new HashMap<>();
      }
      this.additionalProperties$value.put(key, value);
      return this;
    }
  }
}
