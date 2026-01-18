package com.kong.kafka.consumer;

import com.kong.kafka.exception.KafkaConsumerException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

/**
 * Implementation of Kafka Consumer Client with built-in retry and error handling. Provides a simple
 * API for teams to consume messages from Kafka.
 */
@Slf4j
public class KafkaConsumerClientImpl<V> implements KafkaConsumerClient<V> {

  private final KafkaConsumer<String, V> consumer;
  private final KafkaConsumerConfig config;
  private final MessageProcessor<V> messageProcessor;
  private final Retry retry;
  private final AtomicBoolean running;

  public KafkaConsumerClientImpl(
      KafkaConsumer<String, V> consumer,
      KafkaConsumerConfig config,
      MessageProcessor<V> messageProcessor) {
    this.consumer = consumer;
    this.config = config;
    this.messageProcessor = messageProcessor;
    this.running = new AtomicBoolean(false);

    // Setup Resilience4j Retry for message processing with DLQ logging
    RetryConfig retryConfig =
        RetryConfig.custom()
            .maxAttempts(config.getMaxRetryAttempts())
            .waitDuration(Duration.ofMillis(config.getRetryWaitDurationMs()))
            .retryExceptions(Exception.class)
            .build();

    RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
    this.retry = retryRegistry.retry("kafkaConsumer");

    // Add event listener for retry exhaustion
    retry
        .getEventPublisher()
        .onRetry(
            event ->
                log.warn(
                    "Retry attempt {} for message processing", event.getNumberOfRetryAttempts()))
        .onError(
            event -> {
              log.error(
                  " All retry attempts exhausted ({} attempts) for message processing. Message"
                      + " should be written to Dead Letter Queue (DLQ) for manual processing."
                      + " Error: {}",
                  event.getNumberOfRetryAttempts(),
                  event.getLastThrowable().getMessage());
            });

    log.info("KafkaConsumerClient initialized with config: {}", config);
  }

  /**
   * Subscribe to topics and start consuming messages. This method blocks until shutdown() is
   * called.
   *
   * @param topics The topics to subscribe to
   */
  public void subscribe(String... topics) {
    subscribe(Arrays.asList(topics));
  }

  /** Subscribe to topics and start consuming messages. */
  public void subscribe(Collection<String> topics) {
    consumer.subscribe(topics);
    log.info("Subscribed to topics: {}", topics);

    running.set(true);

    try {
      while (running.get()) {
        ConsumerRecords<String, V> records = consumer.poll(Duration.ofMillis(1000));

        if (records.isEmpty()) {
          continue;
        }

        log.debug("Polled {} records", records.count());

        for (ConsumerRecord<String, V> record : records) {
          processRecord(record);
        }

        // Commit offsets after successful processing
        consumer.commitSync();
        log.debug("Committed offsets");
      }
    } catch (WakeupException e) {
      log.info("Consumer wakeup called, shutting down gracefully");
    } catch (Exception e) {
      log.error("Unexpected error in consumer loop", e);
      throw new KafkaConsumerException("Consumer loop failed", e);
    } finally {
      close();
    }
  }

  /** Process a single record with retry and circuit breaker. */
  private void processRecord(ConsumerRecord<String, V> record) {

    Supplier<Void> processOperation =
        () -> {
          try {
            messageProcessor.process(record);
            log.debug(
                "Processed record from topic={}, partition={}, offset={}",
                record.topic(),
                record.partition(),
                record.offset());
            return null;
          } catch (Exception e) {
            log.error(
                "Failed to process record from topic={}, partition={}, offset={}",
                record.topic(),
                record.partition(),
                record.offset(),
                e);
            throw new KafkaConsumerException(record.topic(), "Message processing failed", e);
          }
        };

    // Apply retry - will log DLQ message on exhaustion
    Supplier<Void> retriedOperation = Retry.decorateSupplier(retry, processOperation);

    try {
      retriedOperation.get();
    } catch (Exception e) {
      log.error(
          "Failed to process record after all retries, skipping message: topic={}, partition={},"
              + " offset={}",
          record.topic(),
          record.partition(),
          record.offset(),
          e);
      // Message already logged for DLQ in retry event listener
    }
  }

  /** Shutdown the consumer gracefully. */
  public void shutdown() {
    log.info("Shutting down Kafka consumer");
    running.set(false);
    consumer.wakeup();
  }

  @Override
  public void close() {
    if (consumer != null) {
      log.info("Closing Kafka consumer");
      consumer.close();
    }
  }
}
