package com.kong.kafka.exception;

/** Exception thrown when there is an error consuming messages from Kafka. */
public class KafkaConsumerException extends RuntimeException {

  private final String topic;

  public KafkaConsumerException(String message) {
    super(message);
    this.topic = null;
  }

  public KafkaConsumerException(String message, Throwable cause) {
    super(message, cause);
    this.topic = null;
  }

  public KafkaConsumerException(String topic, String message, Throwable cause) {
    super(String.format("Failed to consume from topic '%s': %s", topic, message), cause);
    this.topic = topic;
  }

  public String getTopic() {
    return topic;
  }
}
