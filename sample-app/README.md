# Example Application

This example demonstrates how application teams can use the `kafka-common-lib` to:

1. Define schemas
2. Produce data to Kafka
3. Consume data from Kafka
4. Send consumed data to external systems (Elasticsearch)

## Quick Start

### 1. Start Infrastructure

From the root directory:

```bash
docker-compose up -d
```

This starts Kafka, Schema Registry, Kafka UI

### 2. Build the Example

```bash
mvn clean package
```

### 3. Run Producer

```bash
mvn exec:java -Dexec.mainClass="com.kong.example.ExampleProducerApp"
```

This will:
- Register the `UserEvent` schema with Schema Registry
- Produce 5 sample user events to the `user-events` topic
- Demonstrate both sync and async message sending

### 4. Run Consumer

In another terminal:

```bash
mvn exec:java -Dexec.mainClass="com.kong.example.ExampleConsumerApp"
```

This will:
- Consume messages from the `user-events` topic
- Process each message with automatic retry
- (Optionally) Send to Elasticsearch

Press `Ctrl+C` to stop gracefully.

## What's Included

### Schema Definition

`src/main/resources/avro/user-event-v1.avsc`:
- Defines the structure of user events
- Supports multiple event types (CREATED, UPDATED, LOGIN, etc.)
- Includes optional fields and metadata

### Producer Example

`ExampleProducerApp.java`:
- Shows how to register schemas
- Demonstrates synchronous message sending
- Demonstrates asynchronous message sending
- Shows error handling with automatic retry

### Consumer Example

`ExampleConsumerApp.java`:
- Shows how to consume and process messages
- Demonstrates sending to external APIs
- Shows graceful shutdown handling
- Includes retry logic for downstream failures

## Customization

### Change Kafka Configuration

Edit the constants in `ExampleProducerApp` and `ExampleConsumerApp`:

```java
private static final String BOOTSTRAP_SERVERS = "localhost:9092";
private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
```

### Define Your Own Schema

1. Create a new `.avsc` file in `src/main/resources/avro/`
2. Run `mvn generate-sources` to generate Java classes
3. Use the generated classes in your producer/consumer

### Send to Different Destination

Modify the `sendToElasticsearch()` method in `ExampleConsumerApp` to send to your destination:

```java
private void sendToYourAPI(UserEvent event) throws Exception {
    // Your custom logic here
}
```

## Monitoring

### Kafka UI

Open http://localhost:8080 to:
- View topics and messages
- Monitor consumer groups
- Check schema registry

### View Messages

```bash
# List topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Consume messages
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic user-events \
  --from-beginning
```

### Check Schema Registry

```bash
# List subjects
curl http://localhost:8081/subjects

# Get schema
curl http://localhost:8081/subjects/user-events-value/versions/latest
```

## Troubleshooting

### Schema Registration Fails

Check if Schema Registry is running:
```bash
curl http://localhost:8081/subjects
```

### Cannot Connect to Kafka

Check if Kafka is running:
```bash
docker ps | grep kafka
```

### Messages Not Appearing

1. Check producer logs for errors
2. Verify topic was created: `docker exec kafka kafka-topics --list --bootstrap-server localhost:9092`
3. Check Kafka UI at http://localhost:8080
