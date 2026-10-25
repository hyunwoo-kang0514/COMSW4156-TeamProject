#!/usr/bin/env bash
# AI-generated (Claude Code). See README "AI Documentation" section.
#
# cURL-based API test for the find-compatible-groups and join-group endpoints.
#
# Usage:
#   1. Start the app locally against the H2 fixture data (no Postgres needed):
#        SEED_PATH="$(pwd)/src/test/resources/local-seed.sql"
#        ./mvnw spring-boot:run -Dspring-boot.run.useTestClasspath=true \
#          -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:mem:devdb;DB_CLOSE_DELAY=-1 \
#          --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa \
#          --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=create-drop \
#          --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
#          --spring.jpa.defer-datasource-initialization=true --spring.sql.init.mode=always \
#          --spring.sql.init.data-locations=file:$SEED_PATH --server.port=8081"
#   2. In another terminal: ./scripts/api-test-find-join.sh
#
# Fixture data (src/test/resources/local-seed.sql):
#   student 1 "hk3382" (api key test-key-1) has a preference for COMS4156/in-person.
#   student 2 "nopref1" (api key test-key-2) has no preference registered.
#   group 1 has room (1/4 members) and overlaps student 1's schedule/style.
#   group 2 is already full (2/2 members).

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
PASS=0
FAIL=0

check() {
  local description="$1"
  local expected_status="$2"
  local actual_status="$3"
  if [[ "$actual_status" == "$expected_status" ]]; then
    echo "PASS: $description (HTTP $actual_status)"
    PASS=$((PASS + 1))
  else
    echo "FAIL: $description (expected $expected_status, got $actual_status)"
    FAIL=$((FAIL + 1))
  fi
}

echo "== /api/find =="

status=$(curl -s -o /tmp/find_ok.json -w "%{http_code}" -X GET "$BASE_URL/api/find?studentId=1" -H "X-API-KEY: test-key-1")
check "find with valid key and registered preference returns 200" 200 "$status"
cat /tmp/find_ok.json; echo

status=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/find?studentId=1" -H "X-API-KEY: bogus-key")
check "find with invalid API key returns 401" 401 "$status"

status=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/find?studentId=999" -H "X-API-KEY: test-key-1")
check "find with nonexistent student returns 404" 404 "$status"

status=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/find?studentId=2" -H "X-API-KEY: test-key-2")
check "find with no registered preference returns 400" 400 "$status"

echo
echo "== /api/join =="

status=$(curl -s -o /tmp/join_ok.json -w "%{http_code}" -X POST "$BASE_URL/api/join" \
  -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" \
  -d '{"studentId":1,"groupId":1}')
check "join with valid student/group returns 200" 200 "$status"
cat /tmp/join_ok.json; echo

status=$(curl -s -o /tmp/join_retry.json -w "%{http_code}" -X POST "$BASE_URL/api/join" \
  -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" \
  -d '{"studentId":1,"groupId":1}')
check "repeating a join for an existing member is idempotent (returns 200)" 200 "$status"

status=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/join" \
  -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" \
  -d '{"studentId":2,"groupId":2}')
check "join on a full group returns 400" 400 "$status"

status=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/join" \
  -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" \
  -d '{"studentId":1,"groupId":999}')
check "join on a nonexistent group returns 400" 400 "$status"

status=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/join" \
  -H "X-API-KEY: bogus-key" -H "Content-Type: application/json" \
  -d '{"studentId":1,"groupId":1}')
check "join with invalid API key returns 401" 401 "$status"

echo
echo "Results: $PASS passed, $FAIL failed"
[[ "$FAIL" -eq 0 ]]
