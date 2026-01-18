package com.kong.kafka.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kong.kafka.exception.SchemaValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.specific.SpecificRecord;

/**
 * Validator for schema validation before publishing to Kafka. Ensures messages conform to the
 * registered schema.
 */
@Slf4j
public class SchemaValidator {

  private final SchemaRegistryProvider schemaRegistryProvider;
  private final ObjectMapper objectMapper;

  public SchemaValidator(SchemaRegistryProvider schemaRegistryProvider) {
    this.schemaRegistryProvider = schemaRegistryProvider;
    this.objectMapper = new ObjectMapper();
  }

  /** Validate an Avro SpecificRecord against its schema. */
  public void validateAvroRecord(String subject, SpecificRecord record) {
    try {
      Schema recordSchema = record.getSchema();
      Schema registrySchema = schemaRegistryProvider.getLatestSchema(subject);

      if (!recordSchema.equals(registrySchema)) {
        // Check compatibility
        if (!schemaRegistryProvider.isCompatible(subject, recordSchema)) {
          throw new SchemaValidationException(
              subject, record, "Schema is not compatible with the registered schema");
        }
      }

      log.debug("Successfully validated Avro record for subject: {}", subject);
    } catch (Exception e) {
      log.error("Schema validation failed for subject: {}", subject, e);
      throw new SchemaValidationException(subject, record, "Validation failed", e);
    }
  }

  /** Validate a GenericRecord against a schema. */
  public void validateGenericRecord(String subject, GenericRecord record) {
    try {
      Schema recordSchema = record.getSchema();
      Schema registrySchema = schemaRegistryProvider.getLatestSchema(subject);

      if (!recordSchema.equals(registrySchema)) {
        if (!schemaRegistryProvider.isCompatible(subject, recordSchema)) {
          throw new SchemaValidationException(
              subject, record, "Schema is not compatible with the registered schema");
        }
      }

      log.debug("Successfully validated Generic record for subject: {}", subject);
    } catch (Exception e) {
      log.error("Schema validation failed for subject: {}", subject, e);
      throw new SchemaValidationException(subject, record, "Validation failed", e);
    }
  }

  /** Validate JSON data against an Avro schema. */
  public void validateJsonAgainstAvroSchema(String subject, String jsonData) {
    try {
      Schema schema = schemaRegistryProvider.getLatestSchema(subject);

      // Try to parse JSON against Avro schema
      JsonDecoder decoder = DecoderFactory.get().jsonDecoder(schema, jsonData);
      DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
      reader.read(null, decoder);

      log.debug("Successfully validated JSON data for subject: {}", subject);
    } catch (Exception e) {
      log.error("JSON validation failed for subject: {}", subject, e);
      throw new SchemaValidationException(
          subject, jsonData, "JSON does not conform to Avro schema", e);
    }
  }

  /** Validate that required fields are present. */
  public void validateRequiredFields(String subject, Object data, String... requiredFields) {
    try {
      JsonNode jsonNode = objectMapper.valueToTree(data);

      for (String field : requiredFields) {
        if (!jsonNode.has(field) || jsonNode.get(field).isNull()) {
          throw new SchemaValidationException(
              subject, data, String.format("Required field '%s' is missing or null", field));
        }
      }

      log.debug("Successfully validated required fields for subject: {}", subject);
    } catch (SchemaValidationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Required field validation failed for subject: {}", subject, e);
      throw new SchemaValidationException(subject, data, "Required field validation failed", e);
    }
  }
}
