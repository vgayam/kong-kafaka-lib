package com.kong.kafka.producer;

import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * Interface for Kafka Producer Client. Schema validation is handled automatically by the serializer
 * (e.g., KafkaAvroSerializer).
 *
 * @param <V> The type of message values
 */
public interface KafkaProducerClient<V> extends AutoCloseable {

  /**
   * Send a message synchronously with automatic retry.
   *
   * @param topic The Kafka topic
   * @param key The message key (for partitioning)
   * @param value The message value
   * @return RecordMetadata with partition and offset information
   */
  RecordMetadata sendSync(String topic, String key, V value);

  /** Flush all buffered messages. */
  void flush();

  /** Close the producer and release resources. */
  @Override
  void close();
}
