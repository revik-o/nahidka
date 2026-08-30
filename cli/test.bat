@echo off
cd /d "%~dp0\.."
cd back-end && mise exec -- make test && cd ..
cd client && mise exec -- make test && cd ..
