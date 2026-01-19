#!/bin/bash

echo "Stopping Kafka Common Library services..."
echo ""

# Detect compose command
COMPOSE_CMD=""

if command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
elif command -v docker &> /dev/null && docker compose version &> /dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
elif command -v nerdctl &> /dev/null && nerdctl compose version &> /dev/null 2>&1; then
    COMPOSE_CMD="nerdctl compose"
else
    echo "No compose command found!"
    exit 1
fi

echo "Using: $COMPOSE_CMD"
echo ""

$COMPOSE_CMD down

echo ""
echo " All services stopped"
echo ""
