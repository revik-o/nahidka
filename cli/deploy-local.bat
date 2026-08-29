@echo off
cd /d "%~dp0\..\docker\local"

echo Building and starting containers in detached mode...
docker compose up --build -d

echo Attaching to backend logs (Press Ctrl+C to stop, and press N when prompted to Terminate batch job to allow cleanup)...
docker logs -f nahidka-backend

echo.
echo Cleaning up containers and local backend image...
docker compose down
echo Removing nahidka-backend:local image...
docker rmi nahidka-backend:local
