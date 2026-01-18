package com.kong.kafka.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Utility class for building Kafka configuration properties. Provides sensible defaults and best
 * practices.
 */
public class KafkaConfigUtils {

  private KafkaConfigUtils() {
    // Utility class
  }

  /** Build default producer configuration with best practices. */
  public static Properties buildProducerConfig(
      String bootstrapServers, Map<String, Object> additionalConfig) {
    Properties props = new Properties();
    // Basic configuration
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    // Performance tuning
    props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
    props.put(ProducerConfig.LINGER_MS_CONFIG, "10");
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, "32768");
    // Timeouts
    props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");
    props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "120000");
    // Add additional configuration
    if (additionalConfig != null) {
      props.putAll(additionalConfig);
    }
    return props;
  }

  /** Build default consumer configuration with best practices. */
  public static Properties buildConsumerConfig(
      String bootstrapServers, String groupId, Map<String, Object> additionalConfig) {
    Properties props = new Properties();

    // Basic configuration
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    // Auto commit disabled for manual control
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    // Consumer behavior
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500");
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000");
    // Session timeout
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
    props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "3000");
    // Add additional configuration
    if (additionalConfig != null) {
      props.putAll(additionalConfig);
    }
    return props;
  }

  /** Merge two property sets, with override taking precedence. */
  public static Properties mergeProperties(Properties base, Properties override) {
    Properties merged = new Properties();
    merged.putAll(base);
    if (override != null) {
      merged.putAll(override);
    }
    return merged;
  }

  /** Convert Properties to Map for easier manipulation. */
  public static Map<String, Object> propertiesToMap(Properties properties) {
    Map<String, Object> map = new HashMap<>();
    properties.forEach((key, value) -> map.put(key.toString(), value));
    return map;
  }
}
