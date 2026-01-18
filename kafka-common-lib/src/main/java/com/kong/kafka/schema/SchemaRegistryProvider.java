package com.kong.kafka.schema;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;

/**
 * Provider for Schema Registry client operations. Handles schema registration, retrieval, and
 * caching.
 */
@Slf4j
public class SchemaRegistryProvider {

  private final SchemaRegistryClient schemaRegistryClient;
  private final Map<String, Schema> schemaCache;

  public SchemaRegistryProvider(String schemaRegistryUrl) {
    this(schemaRegistryUrl, 100);
  }

  public SchemaRegistryProvider(String schemaRegistryUrl, int cacheCapacity) {
    this.schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryUrl, cacheCapacity);
    this.schemaCache = new HashMap<>();
    log.info("Initialized SchemaRegistryProvider with URL: {}", schemaRegistryUrl);
  }

  public SchemaRegistryProvider(SchemaRegistryClient client) {
    this.schemaRegistryClient = client;
    this.schemaCache = new HashMap<>();
  }

  /**
   * Register a schema for a given subject.
   *
   * @param subject The subject name (typically topic-value or topic-key)
   * @param schema The Avro schema to register
   * @return The schema ID
   */
  public int registerSchema(String subject, Schema schema) {
    try {
      int schemaId = schemaRegistryClient.register(subject, schema);
      schemaCache.put(subject, schema);
      log.info("Registered schema for subject '{}' with ID: {}", subject, schemaId);
      return schemaId;
    } catch (IOException | RestClientException e) {
      log.error("Failed to register schema for subject: {}", subject, e);
      throw new RuntimeException("Failed to register schema", e);
    }
  }

  /** Get the latest schema for a subject. */
  public Schema getLatestSchema(String subject) {
    if (schemaCache.containsKey(subject)) {
      return schemaCache.get(subject);
    }

    try {
      String schemaString = schemaRegistryClient.getLatestSchemaMetadata(subject).getSchema();
      Schema schema = new Schema.Parser().parse(schemaString);
      schemaCache.put(subject, schema);
      return schema;
    } catch (IOException | RestClientException e) {
      log.error("Failed to get latest schema for subject: {}", subject, e);
      throw new RuntimeException("Failed to get schema", e);
    }
  }

  /** Get a specific version of a schema. */
  public Schema getSchemaByVersion(String subject, int version) {
    try {
      String schemaString = schemaRegistryClient.getSchemaMetadata(subject, version).getSchema();
      return new Schema.Parser().parse(schemaString);
    } catch (IOException | RestClientException e) {
      log.error("Failed to get schema for subject '{}' version {}", subject, version, e);
      throw new RuntimeException("Failed to get schema", e);
    }
  }

  /** Get schema by ID. */
  public Schema getSchemaById(int schemaId) {
    try {
      String schemaString = schemaRegistryClient.getSchemaById(schemaId).toString();
      return new Schema.Parser().parse(schemaString);
    } catch (IOException | RestClientException e) {
      log.error("Failed to get schema by ID: {}", schemaId, e);
      throw new RuntimeException("Failed to get schema", e);
    }
  }

  /** Check if a schema is compatible with the latest version. */
  public boolean isCompatible(String subject, Schema schema) {
    try {
      return schemaRegistryClient.testCompatibility(subject, schema);
    } catch (IOException | RestClientException e) {
      log.error("Failed to check compatibility for subject: {}", subject, e);
      return false;
    }
  }

  /** Get the schema registry client for advanced operations. */
  public SchemaRegistryClient getClient() {
    return schemaRegistryClient;
  }
}
