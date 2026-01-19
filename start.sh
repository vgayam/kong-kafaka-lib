#!/bin/bash

echo "🚀 Starting Kafka Common Library Setup..."
echo ""

# Detect container runtime
CONTAINER_RUNTIME=""
COMPOSE_CMD=""

# Check for Rancher Desktop (nerdctl)
if command -v nerdctl &> /dev/null && nerdctl info &> /dev/null 2>&1; then
    CONTAINER_RUNTIME="nerdctl"
    echo " Rancher Desktop detected"
    if nerdctl compose version &> /dev/null 2>&1; then
        COMPOSE_CMD="nerdctl compose"
    fi
# Check for Docker
elif command -v docker &> /dev/null && docker info &> /dev/null 2>&1; then
    CONTAINER_RUNTIME="docker"
    echo "Docker detected"
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null 2>&1; then
        COMPOSE_CMD="docker compose"
    fi
else
    echo "No container runtime detected!"
    echo ""
    echo "Please install Rancher Desktop:"
    echo "  1. Go to: https://rancherdesktop.io/"
    echo "  2. Download and install for macOS"
    echo "  3. Open Rancher Desktop and wait for it to start"
    echo "  4. Run this script again"
    echo ""
    echo "Or install Docker Desktop:"
    echo "  brew install --cask docker"
    echo ""
    exit 1
fi

if [ -z "$COMPOSE_CMD" ]; then
    echo " Compose command not found!"
    exit 1
fi

echo "Using: $COMPOSE_CMD"
echo ""

# Start Docker Compose services
echo "📦 Starting Kafka, Schema Registry, and supporting services..."
$COMPOSE_CMD up -d

echo "⏳ Waiting for services to be ready..."
sleep 10

# Check if Kafka is ready
echo "🔍 Checking Kafka..."
until $CONTAINER_RUNTIME exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
    echo "   Waiting for Kafka..."
    sleep 5
done
echo " Kafka is ready"

# Check if Schema Registry is ready
echo "🔍 Checking Schema Registry..."
until curl -s http://localhost:8081/subjects > /dev/null; do
    echo "   Waiting for Schema Registry..."
    sleep 5
done
echo "Schema Registry is ready"

echo ""
echo "All services are ready!"
echo ""
echo "Kafka UI: http://localhost:8080"
echo "Schema Registry: http://localhost:8081"
echo ""
echo "Next steps:"
echo "  1. Build the project: mvn clean install"
echo "  2. Run producer: cd sample-app && mvn exec:java -Dexec.mainClass='com.kong.sample.SampleProducerApp'"
echo "  3. Run consumer: cd sample-app && mvn exec:java -Dexec.mainClass='com.kong.sample.SampleConsumerApp'"
echo ""
