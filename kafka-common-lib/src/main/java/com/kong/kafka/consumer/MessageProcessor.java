package com.kong.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Interface for message processing logic. Application teams implement this to define how messages
 * should be processed.
 *
 * @param <V> The type of the message value
 */
@FunctionalInterface
public interface MessageProcessor<V> {

  /**
   * Process a single message from Kafka.
   *
   * @param record The Kafka consumer record
   * @throws Exception if processing fails (will trigger retry)
   */
  void process(ConsumerRecord<String, V> record) throws Exception;
}
