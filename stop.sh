#!/bin/bash

echo "🛑 Stopping Secure Messenger..."

# Stop Spring Boot application
if [ -f app.pid ]; then
    APP_PID=$(cat app.pid)
    if ps -p $APP_PID > /dev/null 2>&1; then
        echo "Stopping Spring Boot application (PID: $APP_PID)..."
        kill $APP_PID
        wait $APP_PID 2>/dev/null
        echo "✅ Application stopped"
    fi
    rm app.pid
fi

# Stop Docker containers
echo "Stopping Docker containers..."
docker-compose -f docker-compose.prod.yml down

echo "✅ All services stopped"
