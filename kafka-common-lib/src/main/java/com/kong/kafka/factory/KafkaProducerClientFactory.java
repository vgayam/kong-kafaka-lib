package com.kong.kafka.factory;

import com.kong.kafka.producer.KafkaProducerClient;
import com.kong.kafka.producer.KafkaProducerClientImpl;
import com.kong.kafka.producer.KafkaProducerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;

/** Factory for creating KafkaProducerClient instances. */
public class KafkaProducerClientFactory {

  private KafkaProducerClientFactory() {
    // Factory class
  }

  /** Create a Kafka producer client with Avro serialization. */
  public static <V> KafkaProducerClient<V> createAvroProducer(KafkaProducerConfig config) {
    Properties props = config.toProperties();
    props.put("value.serializer", KafkaAvroSerializer.class.getName());

    KafkaProducer<String, V> producer = new KafkaProducer<>(props);

    return new KafkaProducerClientImpl<>(producer, config);
  }

  /** Create a Kafka producer client with String serialization. */
  public static KafkaProducerClient<String> createStringProducer(KafkaProducerConfig config) {
    Properties props = config.toProperties();
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

    KafkaProducer<String, String> producer = new KafkaProducer<>(props);

    return new KafkaProducerClientImpl<>(producer, config);
  }

  /** Create a custom producer with provided serializer. */
  public static <V> KafkaProducerClient<V> createCustomProducer(
      KafkaProducerConfig config, String valueSerializerClass) {
    Properties props = config.toProperties();
    props.put("value.serializer", valueSerializerClass);

    KafkaProducer<String, V> producer = new KafkaProducer<>(props);

    return new KafkaProducerClientImpl<>(producer, config);
  }
}
