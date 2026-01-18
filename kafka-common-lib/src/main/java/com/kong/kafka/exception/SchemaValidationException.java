package com.kong.kafka.exception;

/**
 * Exception thrown when schema validation fails. This prevents invalid messages from being
 * published to Kafka.
 */
public class SchemaValidationException extends RuntimeException {

  private final String schemaSubject;
  private final Object invalidMessage;

  public SchemaValidationException(String message) {
    super(message);
    this.schemaSubject = null;
    this.invalidMessage = null;
  }

  public SchemaValidationException(String message, Throwable cause) {
    super(message, cause);
    this.schemaSubject = null;
    this.invalidMessage = null;
  }

  public SchemaValidationException(String schemaSubject, Object invalidMessage, String reason) {
    super(String.format("Schema validation failed for subject '%s': %s", schemaSubject, reason));
    this.schemaSubject = schemaSubject;
    this.invalidMessage = invalidMessage;
  }

  public SchemaValidationException(
      String schemaSubject, Object invalidMessage, String reason, Throwable cause) {
    super(
        String.format("Schema validation failed for subject '%s': %s", schemaSubject, reason),
        cause);
    this.schemaSubject = schemaSubject;
    this.invalidMessage = invalidMessage;
  }

  public String getSchemaSubject() {
    return schemaSubject;
  }

  public Object getInvalidMessage() {
    return invalidMessage;
  }
}
