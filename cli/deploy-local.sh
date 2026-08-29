#!/bin/bash
cd "$(dirname "$0")/../docker/local"

cleanup() {
    echo ""
    echo "Cleaning up containers and local backend image..."
    docker compose down
    echo "Removing nahidka-backend:local image..."
    docker rmi nahidka-backend:local
}

# Trap SIGINT (Ctrl+C), SIGTERM, and normal script EXIT
trap cleanup SIGINT SIGTERM EXIT

echo "Building and starting containers in detached mode..."
docker compose up --build -d

echo "Attaching to backend logs (Press Ctrl+C to stop and cleanup)..."
docker logs -f nahidka-backend
