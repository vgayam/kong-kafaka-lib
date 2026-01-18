package com.kong.kafka.producer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/** Configuration for Kafka Producer */
@Getter
@Builder
@ToString
public class KafkaProducerConfig {

  private final String bootstrapServers;

  private final String schemaRegistryUrl;

  @Builder.Default private final boolean enableIdempotence = true;

  @Builder.Default private final String acks = "all";

  @Builder.Default private final int retries = 10;

  @Builder.Default private final String compressionType = "snappy";

  @Builder.Default private final int lingerMs = 10;

  @Builder.Default private final int batchSize = 32768;

  @Builder.Default private final int requestTimeoutMs = 30000;

  @Builder.Default private final int deliveryTimeoutMs = 120000;

  @Builder.Default private final Map<String, Object> additionalProperties = new HashMap<>();

  /** Convert to Kafka Properties. */
  public Properties toProperties() {
    Properties props = new Properties();

    props.put("bootstrap.servers", bootstrapServers);
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("enable.idempotence", String.valueOf(enableIdempotence));
    props.put("acks", acks);
    props.put("retries", String.valueOf(retries));
    props.put("compression.type", compressionType);
    props.put("linger.ms", String.valueOf(lingerMs));
    props.put("batch.size", String.valueOf(batchSize));
    props.put("request.timeout.ms", String.valueOf(requestTimeoutMs));
    props.put("delivery.timeout.ms", String.valueOf(deliveryTimeoutMs));

    if (schemaRegistryUrl != null && !schemaRegistryUrl.isEmpty()) {
      props.put("schema.registry.url", schemaRegistryUrl);
    }

    // Add additional properties
    props.putAll(additionalProperties);

    return props;
  }

  public static class KafkaProducerConfigBuilder {
    // Custom builder methods can be added here

    public KafkaProducerConfigBuilder addProperty(String key, Object value) {
      if (this.additionalProperties$value == null) {
        this.additionalProperties$value = new HashMap<>();
      }
      this.additionalProperties$value.put(key, value);
      return this;
    }
  }
}
