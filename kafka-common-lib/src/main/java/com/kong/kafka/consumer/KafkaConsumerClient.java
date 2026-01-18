package com.kong.kafka.consumer;

import java.util.Collection;

/**
 * High-level Kafka Consumer interface for consuming messages.
 *
 * @param <V> The value type (message payload)
 */
public interface KafkaConsumerClient<V> extends AutoCloseable {

  /**
   * Subscribe to one or more topics and start consuming messages. This method blocks until
   * shutdown() is called.
   *
   * @param topics The topics to subscribe to
   * @throws com.kong.kafka.exception.KafkaConsumerException if consumption fails
   */
  void subscribe(String... topics);

  /**
   * Subscribe to a collection of topics and start consuming messages. This method blocks until
   * shutdown() is called.
   *
   * @param topics The collection of topics to subscribe to
   * @throws com.kong.kafka.exception.KafkaConsumerException if consumption fails
   */
  void subscribe(Collection<String> topics);

  /** Shutdown the consumer gracefully. Stops consuming messages and triggers cleanup. */
  void shutdown();

  /** Close the consumer and release resources. */
  @Override
  void close();
}
