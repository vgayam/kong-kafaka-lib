package com.kong.kafka.exception;

/**
 * Exception thrown when there is an error publishing messages to Kafka. This is a runtime exception
 * to allow flexible error handling strategies.
 */
public class KafkaPublishException extends RuntimeException {

  private final String topic;
  private final Object message;

  public KafkaPublishException(String message) {
    super(message);
    this.topic = null;
    this.message = null;
  }

  public KafkaPublishException(String message, Throwable cause) {
    super(message, cause);
    this.topic = null;
    this.message = null;
  }

  public KafkaPublishException(String topic, Object message, Throwable cause) {
    super(String.format("Failed to publish message to topic '%s'", topic), cause);
    this.topic = topic;
    this.message = message;
  }

  public KafkaPublishException(String topic, Object message, String errorMessage, Throwable cause) {
    super(String.format("Failed to publish message to topic '%s': %s", topic, errorMessage), cause);
    this.topic = topic;
    this.message = message;
  }

  public String getTopic() {
    return topic;
  }

  public Object getFailedMessage() {
    return message;
  }
}
