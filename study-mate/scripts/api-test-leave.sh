#!/usr/bin/env bash
# AI-generated (Claude Code), reviewed by Ziyue Fang. See README "AI Documentation" section.
#
# cURL-based API test for the leave-study-group endpoint (POST /api/leave).
#
# Usage:
#   1. Start the app locally against the H2 fixture data (same command as
#      scripts/api-test-find-join.sh, see README "Run locally without a Postgres connection").
#   2. In another terminal: ./scripts/api-test-leave.sh
#
# Uses the fixture data in src/test/resources/local-seed.sql:
#   student 1 "hk3382" (api key test-key-1), student 2 "nopref1" (api key test-key-2).
#   group 1 starts with 1/4 members ("other-student").
# The script first joins student 1 into group 1 so there is a membership to remove.
# Run it on a fresh app start (or after api-test-find-join.sh; both orders work).

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
PASS=0
FAIL=0
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

check() {
  local description="$1"
  local expected="$2"
  local actual="$3"
  if [[ "$actual" == "$expected" ]]; then
    echo "PASS: $description ($actual)"
    PASS=$((PASS + 1))
  else
    echo "FAIL: $description (expected $expected, got $actual)"
    FAIL=$((FAIL + 1))
  fi
}

leave() {
  # leave <api-key> <json-body> <output-file>
  curl -s -o "$3" -w "%{http_code}" -X POST "$BASE_URL/api/leave" \
    -H "X-API-KEY: $1" -H "Content-Type: application/json" -d "$2"
}

json_field() {
  # json_field <file> <field>: crude extractor for a top-level number field
  grep -o "\"$2\":[0-9]*" "$1" | head -1 | cut -d: -f2
}

echo "== setup: student 1 joins group 1 =="
status=$(curl -s -o "$TMP_DIR/join.json" -w "%{http_code}" -X POST "$BASE_URL/api/join" \
  -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" \
  -d '{"studentId":1,"groupId":1}')
check "setup join returns 200" 200 "$status"
before=$(json_field "$TMP_DIR/join.json" numMembers)
echo "numMembers before leave: $before"

echo
echo "== /api/leave =="

status=$(leave test-key-1 '{"studentId":1,"groupId":1}' "$TMP_DIR/leave_ok.json")
check "leave as a member returns 200" 200 "$status"
cat "$TMP_DIR/leave_ok.json"; echo
after=$(json_field "$TMP_DIR/leave_ok.json" numMembers)
check "numMembers decremented by 1" "$((before - 1))" "$after"
if grep -q '"hk3382"' "$TMP_DIR/leave_ok.json"; then
  check "hk3382 removed from memberIds" "absent" "present"
else
  check "hk3382 removed from memberIds" "absent" "absent"
fi

status=$(leave test-key-1 '{"studentId":1,"groupId":1}' "$TMP_DIR/leave_retry.json")
check "repeating leave is idempotent (returns 200)" 200 "$status"
check "numMembers unchanged on repeat" "$after" "$(json_field "$TMP_DIR/leave_retry.json" numMembers)"

status=$(leave bogus-key '{"studentId":1,"groupId":1}' /dev/null)
check "leave with invalid API key returns 401" 401 "$status"

status=$(leave test-key-1 '{"studentId":1}' /dev/null)
check "leave with missing groupId returns 400" 400 "$status"

status=$(leave test-key-1 '{"studentId":1,"groupId":999}' /dev/null)
check "leave a nonexistent group returns 404" 404 "$status"

status=$(leave test-key-1 '{"studentId":999,"groupId":1}' /dev/null)
check "leave as a nonexistent student returns 404" 404 "$status"

status=$(leave test-key-2 '{"studentId":1,"groupId":1}' /dev/null)
check "removing a different student returns 403" 403 "$status"

echo
echo "Results: $PASS passed, $FAIL failed"
[[ "$FAIL" -eq 0 ]]
