package com.kong.kafka.producer;

import com.kong.kafka.exception.KafkaPublishException;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * Implementation of Kafka Producer Client. Provides a simple API for teams to publish messages to
 * Kafka.
 */
@Slf4j
public class KafkaProducerClientImpl<V> implements KafkaProducerClient<V> {

  private final KafkaProducer<String, V> producer;

  public KafkaProducerClientImpl(KafkaProducer<String, V> producer, KafkaProducerConfig config) {
    this.producer = producer;
    log.info("KafkaProducerClient initialized with config: {}", config);
  }

  /**
   * Send a message synchronously with automatic retry. Schema validation is handled automatically
   * by KafkaAvroSerializer. Kafka's built-in retry mechanism (configured via retries property)
   * handles transient failures.
   *
   * @param topic The Kafka topic
   * @param key The message key (for partitioning)
   * @param value The message value
   * @return RecordMetadata with partition and offset information
   */
  public RecordMetadata sendSync(String topic, String key, V value) {
    try {
      ProducerRecord<String, V> record = new ProducerRecord<>(topic, key, value);
      Future<RecordMetadata> future = producer.send(record);
      RecordMetadata metadata = future.get();

      log.debug(
          "Message sent to topic={}, partition={}, offset={}",
          metadata.topic(),
          metadata.partition(),
          metadata.offset());

      return metadata;
    } catch (Exception e) {
      log.error("Failed to send message to topic: {} after Kafka retries exhausted", topic, e);
      throw new KafkaPublishException(topic, value, e);
    }
  }

  /** Flush all buffered messages. */
  public void flush() {
    producer.flush();
    log.debug("Producer flushed");
  }

  @Override
  public void close() {
    if (producer != null) {
      log.info("Closing Kafka producer");
      producer.close();
    }
  }
}
