# Kafka Schemas

Shared Avro schemas for Kafka events. This module serves as the **contract** between producers and consumers.

## Purpose

- **Decouples** producers and consumers (they don't depend on each other's code)
- **Versioned** schemas allow backward-compatible evolution
- **Type-safe** Java classes generated from schemas
- **Single source of truth** for message structure

## Structure

```
kafka-schemas/
└── src/main/resources/avro/
    ├── v1/
    │   └── user-event-v1.avsc    (Schema version 1.0)
    └── v2/
        └── user-event-v2.avsc    (Schema version 1.1 - future)
```

## Usage

### For Producer Teams

```xml
<dependency>
    <groupId>com.kong</groupId>
    <artifactId>kafka-schemas</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
import com.kong.schemas.avro.v1.UserEvent;
import com.kong.schemas.avro.v1.EventType;

UserEvent event = UserEvent.newBuilder()
    .setUserId("user-123")
    .setEventType(EventType.CREATED)
    .setTimestamp(System.currentTimeMillis())
    .build();

producer.sendSync("user-events", "key", event);
```

### For Consumer Teams

```xml
<dependency>
    <groupId>com.kong</groupId>
    <artifactId>kafka-schemas</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
import com.kong.schemas.avro.v1.UserEvent;

KafkaConsumerClient<UserEvent> consumer = 
    KafkaConsumerClientFactory.createAvroConsumer(config, record -> {
        UserEvent event = record.value();
        System.out.println("User: " + event.getUserId());
    });
```

## Schema Versioning

### Version Mapping

| Schema File | Library Version | Changes | Compatible? |
|-------------|-----------------|---------|-------------|
| user-event-v1.avsc | 1.0.0 | Initial release | N/A |
| user-event-v2.avsc | 1.1.0 | Add optional field | ✅ Backward |
| user-event-v3.avsc | 2.0.0 | Remove required field | ❌ Breaking |

### Making Changes

**Backward Compatible (Minor version bump):**
- ✅ Add optional fields with defaults
- ✅ Add new enum values

**Breaking Changes (Major version bump):**
- ⚠️ Remove fields
- ⚠️ Change field types
- ⚠️ Remove enum values

### Example: Adding a Field

```json
{
  "name": "UserEvent",
  "fields": [
    {"name": "userId", "type": "string"},
    {"name": "eventType", "type": "..."},
    {"name": "phoneNumber", "type": ["null", "string"], "default": null}
  ]
}
```

1. Add field to schema with `default: null`
2. Bump version: `mvn versions:set -DnewVersion=1.1.0`
3. Deploy: `mvn deploy`
4. Producers can upgrade to `kafka-schemas:1.1.0` when ready
5. Old consumers on `1.0.0` still work (field is optional)

## Building

```bash
# Generate Java classes from schemas
mvn clean install

# Generated classes appear in:
target/generated-sources/avro/com/kong/schemas/avro/v1/UserEvent.java
```

## Teams Can Work Independently

```
Producer Team:
  kafka-common-lib:1.0.0  +  kafka-schemas:1.0.0

Consumer Team A:
  kafka-common-lib:1.0.0  +  kafka-schemas:1.0.0

Consumer Team B (newer):
  kafka-common-lib:1.0.0  +  kafka-schemas:1.1.0

All teams work independently! ✅
```
