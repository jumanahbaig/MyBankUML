@echo off

echo Starting frontend in a new window...
start cmd /k "cd frontend && npm run dev"

echo Starting backend in a new window...
start cmd /k "cd backend && mvn exec:java"

echo Both services started.
