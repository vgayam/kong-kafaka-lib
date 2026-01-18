package com.kong.kafka.producer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.kong.kafka.exception.KafkaPublishException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class KafkaProducerClientTest {

  @Mock private KafkaProducer<String, String> mockProducer;

  private KafkaProducerConfig config;
  private KafkaProducerClientImpl<String> client;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    config =
        KafkaProducerConfig.builder()
            .bootstrapServers("localhost:9092")
            .schemaRegistryUrl("http://localhost:8081")
            .build();

    client = new KafkaProducerClientImpl<>(mockProducer, config);
  }

  @Test
  void testSendSync_Success() throws Exception {
    // Arrange
    String topic = "test-topic";
    String key = "test-key";
    String value = "test-value";

    RecordMetadata expectedMetadata =
        new RecordMetadata(
            new TopicPartition(topic, 0), 0L, 0L, System.currentTimeMillis(), 0L, 0, 0);

    Future<RecordMetadata> future = CompletableFuture.completedFuture(expectedMetadata);
    when(mockProducer.send(any(ProducerRecord.class))).thenReturn(future);

    // Act
    RecordMetadata result = client.sendSync(topic, key, value);

    // Assert
    assertNotNull(result);
    assertEquals(0, result.partition());
    verify(mockProducer, times(1)).send(any(ProducerRecord.class));
  }

  @Test
  void testSendSync_RetryOnFailure() {
    // Arrange
    String topic = "test-topic";
    String key = "test-key";
    String value = "test-value";

    when(mockProducer.send(any(ProducerRecord.class)))
        .thenThrow(new RuntimeException("Connection failed"));

    // Act & Assert
    assertThrows(
        KafkaPublishException.class,
        () -> {
          client.sendSync(topic, key, value);
        });

    // Verify retry happened (3 attempts)
    verify(mockProducer, atLeast(2)).send(any(ProducerRecord.class));
  }

  @Test
  void testClose() {
    // Act
    client.close();

    // Assert
    verify(mockProducer, times(1)).close();
  }

  @Test
  void testFlush() {
    // Act
    client.flush();

    // Assert
    verify(mockProducer, times(1)).flush();
  }
}
