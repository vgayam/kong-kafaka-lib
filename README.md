# Kafka Common Library

A Kafka common library designed for application teams to publish and consume messages. Seamless Developer Experience where teams can onboard quickly without deep Kafka expertise.

## Design Philosophy

Application teams end up re-implementing the same pieces:
serializers, Schema Registry wiring, retries, safe defaults, and shutdown/offset semantics.

This library intentionally splits responsibilities into two independently versioned artifacts:

- **`kafka-common-lib`**: the framework (clients, configuration, retries, safe defaults)
- **`kafka-schemas`**: the data contracts (Avro schemas + generated Java classes)

**Why this abstraction?**
1. **Separation of concerns**: frameworks change slowly; schemas evolve continuously.
2. **Team independence**: producer/consumer teams coordinate via schemas, not shared Kafka code.
3. **Safe defaults**: idempotent producer settings, compression, and retry defaults are baked in.
4. **Type safety**: Avro code generation makes schema mistakes compile-time problems.
5. **Testability**: interfaces and factories make clients easy to mock.

Application teams focus on business logic; the library owns Kafka plumbing.

## Architecture

```
kong-kafka-common-lib/
├── kafka-common-lib/      # Producer/consumer framework
├── kafka-schemas/         # Shared Avro schemas (versioned independently)
└── sample-app/            # Working examples
```

Teams share schemas, not Kafka client code.

---

## For Application Teams

### Your Setup

**1) Add dependencies to your `pom.xml`:**
```xml
<dependency>
  <groupId>com.kong</groupId>
  <artifactId>kafka-common-lib</artifactId>
  <version>1.0.0</version>
</dependency>

<dependency>
  <groupId>com.kong</groupId>
  <artifactId>kafka-schemas</artifactId>
  <version>1.0.0</version>  <!-- choose the schema version you want -->
</dependency>
```

**2) Producer example (Avro):**
```java
import com.kong.kafka.factory.KafkaProducerClientFactory;
import com.kong.kafka.producer.KafkaProducerClient;
import com.kong.kafka.producer.KafkaProducerConfig;
import com.kong.schemas.avro.v1.EventType;
import com.kong.schemas.avro.v1.UserEvent;

public class MyProducer {
  private static final String TOPIC = "user-events";
  private final KafkaProducerClient<UserEvent> producer;

  public MyProducer() {
    KafkaProducerConfig config = KafkaProducerConfig.builder()
        .bootstrapServers(System.getenv("KAFKA_BROKERS"))      // localhost:9092
        .schemaRegistryUrl(System.getenv("SCHEMA_REGISTRY"))   // http://localhost:8081
        .build();

    this.producer = KafkaProducerClientFactory.createAvroProducer(config);
  }

  public void publishUserCreated(String userId, String email) {
    UserEvent event = UserEvent.newBuilder()
        .setUserId(userId)
        .setEventType(EventType.CREATED)
        .setTimestamp(System.currentTimeMillis())
        .setEmail(email)
        .build();

    // topic + key + value
    producer.sendSync(TOPIC, userId, event);
  }
}
```

**3) Consumer example (Avro):**
```java
import com.kong.kafka.consumer.KafkaConsumerClient;
import com.kong.kafka.consumer.KafkaConsumerConfig;
import com.kong.kafka.factory.KafkaConsumerClientFactory;
import com.kong.schemas.avro.v1.UserEvent;

public class MyConsumer {
  public void start() {
    KafkaConsumerConfig config = KafkaConsumerConfig.builder()
        .bootstrapServers(System.getenv("KAFKA_BROKERS"))
        .schemaRegistryUrl(System.getenv("SCHEMA_REGISTRY"))
        .groupId("my-service-consumer-group")
        .build();

    KafkaConsumerClient<UserEvent> consumer =
        KafkaConsumerClientFactory.createAvroConsumer(config, record -> {
          UserEvent event = record.value();
          handleUserEvent(event);
        });

    consumer.subscribe("user-events");
  }

  private void handleUserEvent(UserEvent event) {
    // Your business logic
  }
}
```

**What you DON’T write in an app:**
- serializer/deserializer setup
- Schema Registry wiring
- retry boilerplate
- offset commit coordination
- resource shutdown handling

**What you DO control:**
- which schema version you depend on
- your business rules
- environment config (brokers, Schema Registry, creds)

---

## Quick Start

```bash
./start.sh
mvn clean install
cd sample-app
mvn exec:java -Dexec.mainClass="com.kong.sample.SampleProducerApp"
```

Kafka UI: http://localhost:8080

---

## Schema Versioning Strategy

Schemas are versioned **independently** from the framework library.

```
kafka-schemas/
├── v1/user-event-v1.avsc  → com.kong.schemas.avro.v1  (schema line: 1.x)
└── v2/user-event-v2.avsc  → com.kong.schemas.avro.v2  (breaking)
```

**Version bump rules (suggested):**
- **Patch (1.0.0 → 1.0.1)**: bug fixes, no schema changes
- **Minor (1.0.0 → 1.1.0)**: backward-compatible additions (typically optional fields)
- **Major (1.0.0 → 2.0.0)**: breaking changes (remove fields, change types, rename)

**Why this matters:**
- Team A can use `kafka-schemas:1.0.0` with `kafka-common-lib:1.0.0`
- Team B can use `kafka-schemas:1.1.0` with the same `kafka-common-lib:1.0.0`
- Unrelated schema changes don’t force framework upgrades

---

## Failure Handling

The library treats failures differently depending on whether they are **transient** (e.g., network) or **permanent** (e.g., business rules that keep failing).

### Producer Failures

| Failure Type | Who Handles It | Strategy | Outcome |
|--------------|---------------|----------|---------|
| Schema/type mismatch | Compiler/Avro types | Build-time feedback | Build fails |
| Network timeout / broker unavailable | Kafka client | Retries (default is configurable) | Usually transparent |
| Exhausted retries | Application | Exception thrown | Caller decides (retry, alert, etc.) |

**Notes:**
- Idempotent producer settings help avoid duplicate writes on retries.

### Consumer Failures

| Failure Type | Who Handles It | Strategy | Outcome |
|--------------|---------------|----------|---------|
| Deserialization/schema lookup | Confluent client | Fetch schema from Schema Registry | Transparent |
| Business logic failure (DB down) | Library retry wrapper | Retry (default 3 attempts, configurable) | May recover |
| Persistent business failure | Library | Logs details and skips the record | Consumer continues |
| Offset commit | Library | Commit only after successful processing | At-least-once delivery |

**Important:** the current implementation logs a “DLQ/manual recovery” message but does not automatically publish to a separate DLQ topic.

Example log:
```
ERROR - Message failed after retries. Topic: user-events, Partition: 2, Offset: 12345
        Skipping record. Check logs for manual recovery / DLQ forwarding.
```

---

## Examples

See [`sample-app/`](sample-app/) for working code:
- `SampleProducerApp.java`
- `SampleConsumerApp.java`
- `InvalidSchemaDemo.java`

---

## Building from Source

```bash
mvn clean install
mvn test
```

## Project Structure

```
kong-kafka-common-lib/
├── kafka-common-lib/          # Core library (producer/consumer)
├── kafka-schemas/             # Shared schema module
├── sample-app/                # Usage examples
├── docker-compose.yml         # Local Kafka setup
├── start.sh                   # Convenience script
└── README.md
```

## Troubleshooting

- **"Schema not found"** → produce once (or run `SampleProducerApp`) to register schema
- **"Broker unavailable"** → check containers and ports; re-run `./start.sh`
- **Consumer lag** → check Kafka UI at http://localhost:8080
