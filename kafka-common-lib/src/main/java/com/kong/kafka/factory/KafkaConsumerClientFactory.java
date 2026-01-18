package com.kong.kafka.factory;

import com.kong.kafka.consumer.KafkaConsumerClient;
import com.kong.kafka.consumer.KafkaConsumerClientImpl;
import com.kong.kafka.consumer.KafkaConsumerConfig;
import com.kong.kafka.consumer.MessageProcessor;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import java.util.Properties;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/** Factory for creating KafkaConsumerClient instances. */
public class KafkaConsumerClientFactory {

  private KafkaConsumerClientFactory() {
    // Factory class
  }

  /** Create a Kafka consumer client with Avro deserialization. */
  public static <V> KafkaConsumerClient<V> createAvroConsumer(
      KafkaConsumerConfig config, MessageProcessor<V> messageProcessor) {
    Properties props = config.toProperties();
    props.put("value.deserializer", KafkaAvroDeserializer.class.getName());
    props.put("specific.avro.reader", "true");
    KafkaConsumer<String, V> consumer = new KafkaConsumer<>(props);
    return new KafkaConsumerClientImpl<>(consumer, config, messageProcessor);
  }

  /** Create a Kafka consumer client with String deserialization. */
  public static KafkaConsumerClient<String> createStringConsumer(
      KafkaConsumerConfig config, MessageProcessor<String> messageProcessor) {
    Properties props = config.toProperties();
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

    KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
    return new KafkaConsumerClientImpl<>(consumer, config, messageProcessor);
  }

  /** Create a custom consumer with provided deserializer. */
  public static <V> KafkaConsumerClient<V> createCustomConsumer(
      KafkaConsumerConfig config,
      String valueDeserializerClass,
      MessageProcessor<V> messageProcessor) {
    Properties props = config.toProperties();
    props.put("value.deserializer", valueDeserializerClass);

    KafkaConsumer<String, V> consumer = new KafkaConsumer<>(props);
    return new KafkaConsumerClientImpl<>(consumer, config, messageProcessor);
  }
}
