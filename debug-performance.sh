#!/bin/bash

echo "=== PERFORMANCE DEBUG SCRIPT ==="
echo ""

echo "1. Kiểm tra MongoDB connection từ app container:"
docker exec spring_app sh -c "time echo 'db.runCommand({ping:1})' | mongosh mongodb://root:123456@db:27017/appAgentDB?authSource=admin --quiet"
echo ""

echo "2. Kiểm tra DNS resolution:"
docker exec spring_app sh -c "time nslookup db"
echo ""

echo "3. Kiểm tra API response time:"
time curl -X POST http://localhost:8080/agent/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test123"}' \
  -w "\nHTTP Code: %{http_code}\nTotal time: %{time_total}s\n"
echo ""

echo "4. Kiểm tra container resource usage:"
docker stats --no-stream spring_app mongodb_container
echo ""

echo "5. Xem logs gần nhất:"
docker logs spring_app --tail 50
