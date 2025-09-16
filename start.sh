#!/bin/bash

FRONTEND_URL="http://localhost:3000"
POLL_INTERVAL=2  # seconds

# Start docker-compose in detached mode
docker-compose up --build

echo "Waiting for frontend to be fully ready..."

# Poll until HTTP 200
while true; do
    STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" $FRONTEND_URL || echo "000")
    if [ "$STATUS_CODE" -eq 200 ]; then
        break
    fi
    sleep $POLL_INTERVAL
done

# Optional small buffer to ensure full readiness
sleep 2

# Open default browser (cross-platform macOS/Linux)
if command -v xdg-open > /dev/null; then
    xdg-open $FRONTEND_URL
elif command -v open > /dev/null; then
    open $FRONTEND_URL
else
    echo "Frontend is ready at $FRONTEND_URL. Please open it manually."
fi
