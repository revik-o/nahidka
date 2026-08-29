@echo off
cd /d "%~dp0\.."
cd back-end && mise exec -- make build && cd ..
cd client && mise exec -- make build && cd ..
