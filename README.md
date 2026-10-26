# StudyMate — COMSW4156 Team Project

StudyMate is a REST API that helps students at the same school/course find and join compatible study groups based on schedule, study style, and meeting-mode preferences.

**GitHub repository:** https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject

## Authors / Team Members

| Name | UNI | Area of responsibility (this iteration) |
|---|---|---|
| Hyunwoo Kang | hk3382 | Find Compatible Study Groups, Join Study Group |
| Kevin Chuang | hc3689 | Register Student |
| Hyunwoo Kim | hk3418 | Create Study Group |
| Ugorji Benjamin Barrah | ubb2000 | Register/Update Study Preference |
| Ziyue Fang | zf2389 | Leave Study Group |

> Task tracking: GitHub Issues / GitHub Projects on the repository above.
> *(Add the direct Project board URL here and confirm the Mentor/Instructor have been given access.)*

> **Scope note:** this README section documents Hyunwoo Kang's component (Find Compatible Study Groups, Join Study Group) on the `harry/matching` branch, whose changes are now merged from `main` into `creategroups`. The original component-specific figures below are historical. Current merged verification results are in [reports/api-testing.md](reports/api-testing.md).

---

## 0. Requirements and Local Environment

The project uses:

- Java 17
- Spring Boot 4.1.1 (Spring Web, Spring Data JPA, Hibernate 7.4.5)
- Apache Maven 3.10.0 (via the `mvnw`/`mvnw.cmd` wrapper — no local Maven install required)
- Checkstyle 3.6.0 (ruleset: `google_checks.xml`)
- PMD 7.17.0 (via `maven-pmd-plugin` 3.28.0)
- JaCoCo 0.8.14
- JUnit 5, Mockito, AssertJ (test scope)
- PostgreSQL (hosted on Supabase) as the production datastore; H2 (in-memory) for the automated test suite
- GitHub Actions for Continuous Integration

Local environment used for the verification run documented below:

```text
Java:  OpenJDK 17.0.14 LTS (Amazon Corretto)
Maven: Apache Maven 3.10.0
OS:    macOS 14.5 (aarch64)
```

The Maven configuration in `study-mate/pom.xml` targets Java 17. All Maven commands below are run from the `study-mate` directory:

```bash
cd study-mate
```

---

## 1. Implementation Status (this iteration / MVP)

The proposal specifies six operations. This iteration focuses on a vertical slice that is fully implemented, tested, and demoable end-to-end against the real database:

| Endpoint | Method & Path | Status |
|---|---|---|
| Health check | `GET /api/health` | ✅ Implemented |
| Find Compatible Study Groups | `GET /api/find` | ✅ Implemented, unit-tested, API-tested |
| Join Study Group | `POST /api/join` | ✅ Implemented, unit-tested, API-tested |
| Register Student | `POST /api/students` | ✅ Implemented, unit-tested, API-tested |
| Register/Update Study Preference | — | 🚧 Planned (owner: Ugorji Benjamin Barrah) |
| Create Study Group | `POST /api/groups` | Implemented with validation, unit tests, and MockMvc tests |
| Leave Study Group | `POST /api/leave` | ✅ Implemented, unit-tested, API-tested (owner: Ziyue Fang) |

This matches the assignment's MVP requirement: "as many as possible of the API entry points should be operational and demoable" rather than all proposed functionality.

---

## 2. Updates to the Original Proposal

The following business-logic changes were made after the proposal was submitted and are reflected in the current implementation:

1. **Preferred group size is no longer a strict matching requirement.** It is now a ranking factor only — groups with a slightly different size than requested may still be recommended by `findCompatibleStudyGroups`, just with a lower score.
2. **Same-school is now part of the matching criteria.** We assume students want study mates at the same school *and* course, so `findCompatibleStudyGroups` adds this to its filtering/ranking logic.
3. **Joining a group no longer requires the group to be one of the student's current compatible matches.** The match result returned by `findCompatibleStudyGroups` is a recommendation, not a prerequisite. `joinStudyGroup` now only requires that the student and group both exist, the student is not already a member, and the group has available capacity.
4. **Joining a group the student already belongs to is now idempotent.** Per the proposal's retry-safety requirement ("if the student is already a member, the service returns success with the current group record"), a repeat join call returns `200 OK` with the current group state instead of a `400` error.

---

## 3. API Reference

All endpoints are under the base path `/api` and require the `X-API-KEY` header (except `/health`). The API key is validated against a registered student's `apiKey` column.

### `GET /api/health`
Liveness check.
- **Input:** none
- **Output:** `200 OK`, body: `StudyMate API is running`

### POST /api/students

Registers a new student and generates a unique API key.

**Headers:** Content-Type: application/json. No API key required.

**Request body:**
{
  "studentId": "hc3689",
  "name": "Kevin",
  "email": "student@example.com",
  "school": "Columbia",
  "major": "Computer Science",
  "year": 1
}

**Success:** 201 Created — Returns the registered student's information, database ID, and generated API key.

**Errors:**
- 400 Bad Request — Missing or invalid required student information.
- 409 Conflict — Student ID already registered.

**Side effects:** Creates a new student record in the database and generates a unique API key.

**Call-order dependency:** None. Students must register before accessing endpoints that require an API key.

**Duplicate requests:** A second registration using the same student ID returns 409 Conflict without creating another student.

### `GET /api/find?studentId={id}`
Finds up to the top 3 study groups compatible with the student's registered preference.

| | |
|---|---|
| **Headers** | `X-API-KEY: <api key>` |
| **Query param** | `studentId` (Long, required) |
| **Success** | `200 OK` — JSON array of up to 3 `StudyGroup` objects, ranked by compatibility score. Empty array `[]` if no compatible group exists. |
| **Errors** | `401 Unauthorized` — API key is invalid/unknown.<br>`404 Not Found` — no student with that `studentId`.<br>`400 Bad Request` — student exists but has not registered a study preference. |
| **Side effects** | None — read-only. Does not modify student or group records. |
| **Call-order dependency** | The student must already have a registered `Preference` (via the not-yet-implemented `registerPreference`, or inserted directly for this iteration's testing). Study groups must already exist for a non-empty result. |
| **Idempotent?** | Yes — recomputes from current data every call. |

Compatibility scoring: same school/course (+10/25 depending on size match), overlapping availability (+15–30), shared study style (+20–25), same meeting mode (filter, required). See `MatchingService.getScores()`.

### `POST /api/join`
Adds a student to a study group.

| | |
|---|---|
| **Headers** | `X-API-KEY: <api key>`, `Content-Type: application/json` |
| **Body** | `{"studentId": <Long>, "groupId": <Long>}` |
| **Success** | `200 OK` — JSON `StudyGroup` object reflecting the updated membership (or the unchanged group if the student was already a member). |
| **Errors** | `401 Unauthorized` — API key is invalid.<br>`400 Bad Request` — student or group does not exist, or the group has no remaining capacity (`numMembers >= maxMembers` or not recruiting). |
| **Side effects** | On success, the student's ID is appended to the group's `memberIds` and `numMembers` is incremented; the change is persisted to the database (`StudyGroupRepository.save`). |
| **Call-order dependency** | The student and group must already exist (via registration/creation). `findCompatibleStudyGroups` does **not** need to have been called first — the match result is only a recommendation. |
| **Idempotent?** | Yes — calling it again for a student who is already a member returns `200 OK` with the current (unchanged) group instead of erroring or double-adding the member. |

### `POST /api/`
Registers a study preference for an existing student.

| | |
|---|---|
| **Headers** | `X-API-KEY: <api key>`, `Content-Type: application/json` |
| **Body** | JSON `Preference`. `id` must be the `id` of an existing student (the preference shares the student's primary key). Example:<br>`{"id": 1, "course": "COMS4156", "meetingMode": "in-person", "preferredGroupSize": 4, "studyStyles": ["collaborative", "focused"], "availableTimes": [{"day": "MONDAY", "startTime": "14:00", "endTime": "16:00"}]}` |
| **Success** | `201 Created` — JSON `Preference` object as saved. The linked `student` is not included in the response. |
| **Errors** | `401 Unauthorized` — API key is invalid.<br>`400 Bad Request` — missing `X-API-KEY` header or malformed JSON (e.g. an invalid `day` value).<br>`500 Internal Server Error` — no student exists with the given `id` (the service throws a `RuntimeException`, not yet mapped to `404`). |
| **Side effects** | Inserts a row into `study_preferences`, plus one row per entry in `preference_available_times` and `preference_study_styles`. If the student already has a preference, it is replaced. |
| **Call-order dependency** | The student must already exist. Must be called before `GET /api/find` for that student, which returns `400` when no preference is registered. |
| **Idempotent?** | Yes, in effect — repeating the same request leaves the same single preference for that student, since the student's id is the primary key. |

### `PUT /api/{id}`
Updates an existing student's study preference.

| | |
|---|---|
| **Headers** | `X-API-KEY: <api key>`, `Content-Type: application/json` |
| **Path param** | `id` (Long, required) — the student's `id`, which is also the preference's primary key. |
| **Body** | JSON `Preference` with the new values (same shape as `POST /api/`). The body's `id` is ignored; the path `id` decides which preference is updated. |
| **Success** | `200 OK` — JSON `Preference` object reflecting the updated values. |
| **Errors** | `401 Unauthorized` — API key is invalid.<br>`400 Bad Request` — missing `X-API-KEY` header, non-numeric `id`, or malformed JSON.<br>`500 Internal Server Error` — no preference exists for `id` (the service throws a `RuntimeException`, not yet mapped to `404`). |
| **Side effects** | Overwrites `course`, `availableTimes`, `studyStyles`, `preferredGroupSize` and `meetingMode` for that preference. The `availableTimes` and `studyStyles` lists are replaced entirely, not merged. The student link and `id` never change. |
| **Call-order dependency** | The preference must already exist (created via `POST /api/`). |
| **Idempotent?** | Yes — sending the same body again results in the same stored preference. |

### `GET /api/{id}`
Retrieves a student's registered study preference.

| | |
|---|---|
| **Headers** | `X-API-KEY: <api key>` |
| **Path param** | `id` (Long, required) — the student's `id`. |
| **Success** | `200 OK` — JSON `Preference` object for that student. |
| **Errors** | `401 Unauthorized` — API key is invalid.<br>`400 Bad Request` — missing `X-API-KEY` header or non-numeric `id`.<br>`500 Internal Server Error` — no preference exists for `id` (the service throws a `RuntimeException`, not yet mapped to `404`). |
| **Side effects** | None — read-only. |
| **Call-order dependency** | The preference must already exist (created via `POST /api/`). |
| **Idempotent?** | Yes — returns the current stored preference every call. |

### `POST /api/leave`
Removes a student from a study group. *(Owner: Ziyue Fang)*

| | |
|---|---|
| **Headers** | `X-API-KEY: <api key>`, `Content-Type: application/json` |
| **Body** | `{"studentId": <Long>, "groupId": <Long>}` (`studentId` is the student's database `id`, same as `/api/join`) |
| **Success** | `200 OK` — JSON `StudyGroup` object reflecting the updated membership (or the unchanged group if the student was not a member). |
| **Errors** | `400 Bad Request` — body is missing/malformed, or `studentId`/`groupId` is missing.<br>`401 Unauthorized` — API key is invalid/unknown.<br>`403 Forbidden` — the API key is valid but belongs to a different student (a student can only remove themselves).<br>`404 Not Found` — no student or no group with that id. |
| **Check order** | `401` (key) → `400` (body) → `404` (student/group lookup) → `403` (key owner) → membership check. No data is changed unless every check passes. |
| **Side effects** | On success, every occurrence of the student's `studentId` is removed from the group's `memberIds`, `numMembers` is decremented by the number removed (never below 0), and the change is persisted in one transaction (`StudyGroupService.leaveGroup`, `StudyGroupRepository.save`). The group is **not** deleted when its last member leaves, and `isRecruiting` is left unchanged. |
| **Call-order dependency** | The student and group must already exist, and for anything to change the student must have joined the group first (`POST /api/join`). |
| **Idempotent?** | Yes — calling it again after the student has left (or for a student who was never a member) returns `200 OK` with the current group and does not write to the database. Retrying a leave whose response was lost is therefore safe. |

---

## 4. Example Usage Scenarios

**Correct usage — find then join:**
```bash
# 1. Student looks up compatible groups
curl -X GET "http://localhost:8081/api/find?studentId=1" -H "X-API-KEY: test-key-1"
# -> 200, [{ "groupId": 1, "name": "COMS4156 Study Group", ... }]

# 2. Student joins the recommended group
curl -X POST "http://localhost:8081/api/join" \
  -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" \
  -d '{"studentId":1,"groupId":1}'
# -> 200, group JSON with the student added to memberIds
```

**Correct usage — joining a group that wasn't a recommended match:**
```bash
curl -X POST "http://localhost:8081/api/join" \
  -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" \
  -d '{"studentId":1,"groupId":5}'
# -> 200, as long as group 5 exists, student 1 is not already a member,
#    and group 5 has capacity. (Per Proposal Update #3, group 5 did not
#    need to appear in student 1's /api/find results first.)
```

**Correct usage — join then leave:**
```bash
# 1. Student 1 joins group 1
curl -X POST "http://localhost:8081/api/join" \
  -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" \
  -d '{"studentId":1,"groupId":1}'
# -> 200, numMembers 1 -> 2, memberIds gains "hk3382"

# 2. Student 1 leaves group 1
curl -X POST "http://localhost:8081/api/leave" \
  -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" \
  -d '{"studentId":1,"groupId":1}'
# -> 200, numMembers 2 -> 1, "hk3382" removed from memberIds

# 3. Retrying the same leave is safe
# -> 200, group unchanged
```

**Contrived bad usage (and why it fails):**

| Call | Result | Why |
|---|---|---|
| `GET /api/find?studentId=1` with no/invalid `X-API-KEY` header | `401` | API key doesn't match any registered student. |
| `GET /api/find?studentId=99999` (nonexistent student) | `404` | No student row with that ID. |
| `GET /api/find?studentId=2` where student 2 has no `Preference` row | `400` | `findCompatibleStudyGroups` requires a registered preference; there is nothing to match against. |
| `POST /api/join` with `groupId` for a group at `numMembers == maxMembers` | `400` | `StudyGroupService.canJoin()` rejects — no capacity left. |
| `POST /api/join` with a `groupId` that doesn't exist | `400` | `findStudyGroupById` returns null, request is rejected before any mutation. |
| Calling `POST /api/join` twice in a row for the same student/group | Both return `200`, member only added once | Idempotency check (`isMember`) short-circuits the second call before `canJoin`/`joinGroup` run. |
| `POST /api/leave` with `X-API-KEY: test-key-2` and `{"studentId":1,"groupId":1}` | `403` | The key is valid but belongs to student 2; a student can only remove themselves from a group. |
| `POST /api/leave` with `{"studentId":1}` (no `groupId`) | `400` | Both ids are required; rejected before any database lookup. |
| `POST /api/leave` with a `groupId` or `studentId` that doesn't exist | `404` | Lookup returns null, request is rejected before any mutation. |
| Calling `POST /api/leave` twice in a row for the same student/group | Both return `200`, `numMembers` only decremented once | `isMember` is false on the second call, so `leaveGroup` never runs again. |

---

## 5. Build, Run, and Test Instructions

**Tech stack:** Java 17, Spring Boot 4.1.1 (Spring Web, Spring Data JPA), Hibernate, PostgreSQL (hosted on Supabase) for the live datastore, H2 (in-memory) for the automated test suite, Maven for build/dependency management, Lombok for entity boilerplate.

All configuration needed to build/run/test is checked into the repo under `study-mate/` (`pom.xml`, `mvnw`/`mvnw.cmd`, `src/main/resources/application.properties`, `src/test/resources/application.properties`).

### Build
```bash
cd study-mate
./mvnw compile
```

### Run against the real database (Postgres/Supabase)
The main datastore connection is externalized via environment variables (see `src/main/resources/application.properties`):
```bash
cd study-mate
DB_URL="jdbc:postgresql://<host>:5432/<db>" \
DB_USERNAME="<username>" \
DB_PASSWORD="<password>" \
./mvnw spring-boot:run
```
The app starts on port `8080` by default (override with `-Dspring-boot.run.arguments="--server.port=8081"`).

### Run locally without a Postgres connection (H2, for quick manual testing)
```bash
cd study-mate
SEED_PATH="$(pwd)/src/test/resources/local-seed.sql"
./mvnw spring-boot:run -Dspring-boot.run.useTestClasspath=true \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:mem:devdb;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR,DAY \
  --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa \
  --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=create-drop \
  --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
  --spring.jpa.defer-datasource-initialization=true --spring.sql.init.mode=always \
  --spring.sql.init.data-locations=file:$SEED_PATH --server.port=8081"
```
`NON_KEYWORDS=YEAR,DAY` is needed because `year` (on `Student`) and `day` (on `TimeSlot`) are reserved words in H2 2.x; without it Hibernate cannot create the `student` and schedule tables and every request fails. This loads the sample fixture data in `src/test/resources/local-seed.sql` (2 students, 2 study groups) so `/api/find` and `/api/join` can be exercised immediately. H2 is used only for this convenience path and for the automated test suite — it is not the production datastore.

### Run the unit test suite (push-button)
```bash
cd study-mate
./mvnw test
```
Runs all JUnit Jupiter tests via Maven Surefire. The merged verification run includes 141 tests across 9 test classes; see [reports/api-testing.md](reports/api-testing.md). No arguments or manual setup required — Spring profiles point the test run at an in-memory H2 database automatically (`src/test/resources/application.properties`).

### Run the API tests (cURL)
```bash
cd study-mate
# with the app already running per one of the "Run" sections above:
BASE_URL=http://localhost:8081 ./scripts/api-test-find-join.sh
BASE_URL=http://localhost:8081 ./scripts/api-test-leave.sh
```
`api-test-leave.sh` covers `/api/leave` with 11 assertions: it first joins student 1 into group 1, then checks the leave success path (status, `numMembers` decremented, member removed), the idempotent retry, and every documented error status (`400`/`401`/`403`/`404`). The two scripts can be run in either order against the same running instance.
This exercises `/api/find` and `/api/join` over real HTTP with 9 assertions covering the success path, both idempotency cases, and every documented error status (`400`/`401`/`404`). It prints a `PASS`/`FAIL` line per case and a final tally.

### Full verification (tests + style + static analysis + coverage)
```bash
cd study-mate
./mvnw verify
```

---

## 6. Testing

### Unit Testing
- **Framework:** JUnit 5 + Mockito (`org.mockito:mockito-junit-jupiter`, via `spring-boot-starter-webmvc-test`/`spring-boot-starter-data-jpa-test`), AssertJ for fluent assertions.
- **Location:** `study-mate/src/test/java/com/studymate/...` — one test class per service/controller:
  - `Controller/ApiControllerTest.java` — controller-level tests for `/api/find` and `/api/join`, with all four collaborator services mocked via `@Mock`/`@InjectMocks`.
  - `Controller/LeaveStudyGroupControllerTest.java` — controller-level tests for `/api/leave` (9 tests: 401, three 400 cases, two 404 cases, 403, idempotent not-a-member, success).
  - `service/MatchingServiceTest.java`, `service/StudyGroupServiceTest.java` (includes 5 `leaveGroup` tests: removal + save, not-a-member no-op, duplicate ids, count floor at 0, recruiting flag unchanged), `service/StudyMateApiServiceTest.java` — service-level unit tests, mocking the repository layer so no real database is touched.
  - `StudyMateApplicationTests.java` — Spring context load smoke test.
- Related tests are grouped by class (one class per unit under test) with shared `@BeforeEach` setup for fixture data, consistent with the assignment's grouping guidance.
- **Config:** no extra config needed beyond `src/test/resources/application.properties` (point of truth for the H2 test datasource) — picked up automatically by Maven/Spring Boot's test classpath.
- **Run:** `./mvnw test` (push-button, see above).

My final local verification (`./mvnw test`) output:
```text
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.studymate.StudyMateApplicationTests
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- in com.studymate.Controller.ApiControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.studymate.service.StudyMateApiServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.studymate.service.MatchingServiceTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- in com.studymate.service.StudyGroupServiceTest
[INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### API Testing
- **Tool:** cURL, per the proposal.
- **Location:** `study-mate/scripts/api-test-find-join.sh`, with fixture data in `study-mate/src/test/resources/local-seed.sql`.
- **What it covers:** both implemented endpoints end-to-end over real HTTP — success responses, `401`/`404`/`400` error paths, and the join-idempotency behavior described in Section 2.
- This was also run manually against the team's real Supabase Postgres instance (not just H2) to confirm persistence — see Section 7.

### Test Coverage
- **Tool:** JaCoCo (`org.jacoco:jacoco-maven-plugin`), bound to the `verify` phase in `study-mate/pom.xml`.
- **Report location:** `study-mate/target/site/jacoco/index.html` (HTML) and `study-mate/target/site/jacoco/jacoco.csv` (raw), generated by `./mvnw verify`.
- **Result:** **89.7% branch coverage** (61/68 branches covered) on this branch, above the 55% requirement for this iteration.
- **Note:** this number is measured on `harry/matching` only, pre-merge. It will be regenerated once all teammates' branches are merged into `main`, since more untested/partially-tested code (register/create/leave endpoints) will enter the coverage denominator.

---

## 7. Manual End-to-End Verification (against real Postgres/Supabase)

Beyond the automated suite, `/api/find` and `/api/join` were manually run against the team's actual Supabase-hosted PostgreSQL instance (not just the H2 test database) to confirm real persistence:
- Confirmed the Supabase schema's column names (`year`, `day`) match the JPA entity mappings exactly.
- Found and fixed a pre-existing schema bug: `preference_available_times`/`preference_study_styles` each had a second, stale foreign key pointing at an orphaned legacy table (`study_preferences`, 0 rows, unused by any current entity) in addition to the correct one pointing at `preference`. This silently blocked saving a student's available times/study styles. The two stale constraints were dropped (non-destructive — no data loss).
- Called `/api/join` over HTTP, then independently queried Postgres directly (`psql`) and confirmed `study_group.num_members` and `study_group_member_ids` reflected the change — i.e., the join is durably persisted, not just echoed back in the HTTP response.

### Verified commands and results (run against the live Supabase instance, app on port 8080)

Fixture state at the time of this run: student `hk3382` (key `test-key-1`) has a registered preference and is a member of group 1; student `nopref1` (key `test-key-2`) exists but has no preference; group 1 has room (2/4 members); group 2 is full (2/2 members).

| # | Command | Expected | Actual result |
|---|---|---|---|
| 1 | `curl http://localhost:8080/api/health` | `StudyMate API is running` | Matched |
| 2 | `curl -X GET "http://localhost:8080/api/find?studentId=1" -H "X-API-KEY: test-key-1"` | `200` + group 1 JSON | Matched exactly: `[{"course":"COMS4156","groupId":1,"maxMembers":4,"meetingMode":"in-person","memberIds":["other-student","hk3382"],"name":"COMS4156 Study Group","numMembers":2,"recruiting":true,"schedules":[{"day":"MONDAY","endTime":"16:00:00","startTime":"14:00:00"}],"school":"Columbia","studyStyles":["collaborative","focused"]}]` |
| 3 | `curl -w "\n%{http_code}\n" -X GET "http://localhost:8080/api/find?studentId=1" -H "X-API-KEY: bogus"` | `401` | Matched |
| 4 | `curl -w "\n%{http_code}\n" -X GET "http://localhost:8080/api/find?studentId=999" -H "X-API-KEY: test-key-1"` | `404` | Matched |
| 5 | `curl -w "\n%{http_code}\n" -X GET "http://localhost:8080/api/find?studentId=2" -H "X-API-KEY: test-key-2"` | `400` (no preference) | Matched |
| 6 | `curl -X POST "http://localhost:8080/api/join" -H "X-API-KEY: test-key-2" -H "Content-Type: application/json" -d '{"studentId":2,"groupId":1}'` | `200`, `numMembers` 2→3, `memberIds` gains `nopref1` | Matched — confirmed by a direct `psql` query afterward showing `num_members = 3` and `nopref1` in `study_group_member_ids` |
| 7 | `curl -X POST "http://localhost:8080/api/join" -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" -d '{"studentId":1,"groupId":1}'` | `200`, unchanged (idempotent, already a member) | Matched |
| 8 | `curl -w "\n%{http_code}\n" -X POST "http://localhost:8080/api/join" -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" -d '{"studentId":1,"groupId":2}'` | `400` (group 2 full) | Matched |
| 9 | `curl -w "\n%{http_code}\n" -X POST "http://localhost:8080/api/join" -H "X-API-KEY: bogus" -H "Content-Type: application/json" -d '{"studentId":1,"groupId":1}'` | `401` | Matched |

All 9/9 matched the documented contract in Section 3. The same 9 cases are automated in `study-mate/scripts/api-test-find-join.sh`.

---

## 8. Style Checking + Self-Documenting Code

- **Tool:** Checkstyle (`org.apache.maven.plugins:maven-checkstyle-plugin`), ruleset **`google_checks.xml`** (Google Java Style), bound to the `verify` phase in `study-mate/pom.xml`.
- **Report location:** `study-mate/target/checkstyle-result.xml`, generated by `./mvnw verify` or `./mvnw checkstyle:check`.
- Code uses mnemonic identifiers (`findCompatibleStudyGroups`, `isMember`, `canJoin`, `joinGroup`, etc.), Javadoc header comments on every public method describing parameters/return/side effects, and inline comments only where the "why" isn't obvious (e.g., the idempotency short-circuit in `joinStudyGroup`).

Run:
```bash
./mvnw checkstyle:check
```
My final verification produced:
```text
[INFO] You have 0 Checkstyle violations.
[INFO] BUILD SUCCESS
```
(`failsOnError=true` in `pom.xml`, so the build fails if this regresses above 0 errors — the warnings Checkstyle prints for pre-existing indentation style on files outside this component's scope do not count as errors and do not fail the build.)

### Static Analysis
- **Tool:** PMD 7.17.0 (`org.apache.maven.plugins:maven-pmd-plugin` 3.28.0), bound to the `verify` phase.
- **Report location:** `study-mate/target/pmd.xml` (raw) / `study-mate/target/reports/pmd.html` (HTML).

Run:
```bash
./mvnw pmd:check
```
My final verification produced:
```text
[INFO] BUILD SUCCESS
```
with 0 entries in `target/pmd.xml`.

---

## 9. Continuous Integration

**Tool:** GitHub Actions.
**Workflow location:** `.github/workflows/ci.yml`.
**Triggers:** every `push` and `pull_request` targeting `main`.

The workflow spins up a real PostgreSQL 17 service container (matching the production Supabase engine version), waits for it to be healthy, then runs the full Maven verification (tests + Checkstyle + PMD + JaCoCo) against it:

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:17
        env:
          POSTGRES_DB: studymate
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        ports:
          - 5432:5432
        options: >-
          --health-cmd="pg_isready"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=5

    env:
      DB_URL: jdbc:postgresql://localhost:5432/studymate
      DB_USERNAME: postgres
      DB_PASSWORD: postgres

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Run Maven verify
        working-directory: study-mate
        run: ./mvnw verify
```

Workflow status and logs are visible from the repository's **Actions** tab.

---

## 10. Third-Party Code Disclosure

No third-party source code is vendored/copy-pasted into this repository. All external functionality is pulled in as a managed Maven dependency (declared in `study-mate/pom.xml`) and fetched from Maven Central at build time:

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc` | Web framework, JPA/ORM |
| `org.postgresql:postgresql` | PostgreSQL JDBC driver (prod datastore, Supabase) |
| `com.h2database:h2` | In-memory DB for the automated test suite |
| `org.projectlombok:lombok` | Boilerplate reduction (getters/setters) on entity classes |
| `org.mockito:*`, JUnit 5, AssertJ | Testing frameworks |
| `org.jacoco:jacoco-maven-plugin` | Coverage reporting |
| `org.apache.maven.plugins:maven-checkstyle-plugin` (+ Google's `google_checks.xml` ruleset) | Style checking |
| `org.apache.maven.plugins:maven-pmd-plugin` | Static analysis |

---

## 11. Project Management

Tasks are tracked via **GitHub Issues / GitHub Projects** on the repository (https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject).
*(Insert the specific Project board URL here, and confirm the Mentor and Instructor have been granted access, per the assignment requirement.)*

---

## 12. AI Documentation

**Tool used:** Claude Code (Anthropic), via its CLI/terminal integration (model: Claude Opus/Sonnet 5 family, accessed through Hyunwoo Kang's Claude account).

**How it was used (this component — Find Compatible Study Groups / Join Study Group):**
- Generated the full unit test suite in `study-mate/src/test/java/com/studymate/Controller/ApiControllerTest.java` from scratch, based on the existing `MatchingServiceTest`/`StudyGroupServiceTest` Mockito patterns already in the repo.
- Debugged and fixed real bugs found while writing/running those tests and while doing manual API testing:
  - A double-`@RequestBody` parameter bug on `POST /api/join` that would have failed at runtime over real HTTP (fixed by introducing `JoinGroupRequest` DTO — see `study-mate/src/main/java/com/studymate/dto/JoinGroupRequest.java`).
  - `GET /api/find` incorrectly used `@RequestBody` for a GET request's `studentId`; changed to `@RequestParam`.
  - `joinStudyGroup` previously returned `400` when the student was already a member, instead of the idempotent `200` the proposal requires; fixed in `ApiController.java`.
  - `StudyGroupService.joinGroup()` never actually persisted the new membership to the database (missing `repository.save()` call); fixed, and verified live against the real Supabase Postgres instance.
  - A stale/orphaned foreign-key constraint in the production Postgres schema that silently blocked saving preference sub-collections (see Section 7).
- Wrote the local API-test fixture (`study-mate/src/test/resources/local-seed.sql`) and the cURL test script (`study-mate/scripts/api-test-find-join.sh`).
- Drafted and polished this README (English; original proposal content provided in Korean/English by the team was translated/organized into the structure above).

**Representative prompts used:**
- "Based on the code I wrote for APIController write a test code for APIControllerTest"
- "Based on the code I wrote and Postgres DB instance write Curl Command" (while doing this, Claude found and fixed the bugs listed above)
- "Refine README in more formal and structured way"

**All AI-authored or AI-debugged code is marked inline with a comment** (e.g., `// AI-generated (Claude Code)` / `// AI-fixed (Claude Code)`) at the top of each affected file or at the specific changed lines, including test code (`ApiControllerTest.java`, the updated `joinGroup` test in `StudyGroupServiceTest.java`), so changes are traceable even after manual review/edits.

**Estimated token usage (this component, this session):** ~400,000–600,000 tokens (combined input + output, including tool/command output consumed as context). This is a rough self-reported estimate based on session length and complexity, not an exact billing figure — for the precise number, pull actual usage from the Claude Code session cost report (`/cost`) or the Anthropic Console usage dashboard tied to the account used.

### Leave Study Group component (Ziyue Fang)

**Tool used:** Claude (Anthropic), Cowork mode, model Claude Opus 5.5, via Ziyue Fang's Claude account.

**What it generated (all reviewed by Ziyue Fang and marked inline with `AI-generated` comments, following the team convention):**
- `POST /api/leave` handler in `ApiController.java` and `StudyGroupService.leaveGroup()`.
- `dto/LeaveGroupRequest.java`.
- `Controller/LeaveStudyGroupControllerTest.java` and the `leaveGroup` tests at the end of `service/StudyGroupServiceTest.java`.
- `scripts/api-test-leave.sh`.
- The `/api/leave` parts of this README, plus the `NON_KEYWORDS=YEAR,DAY` fix to the local H2 run command (found while running the API tests).

**Representative prompt:** "Implement my part (Leave Study Group): entity/repository/service logic, the endpoint, validation, failure cases, idempotency, unit tests with mocks, API tests, and the README section."

---

## 13. Final Verification (this component, pre-merge)

Run from `study-mate/`, on the `harry/matching` branch, before merging into `main`:

```text
./mvnw verify

Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
Checkstyle violations: 0
PMD violations: 0
Branch coverage: 89.7% (61/68)
BUILD SUCCESS
```

Plus the manual cURL verification against the live Supabase Postgres instance in Section 7: **9/9 cases matched** the documented contract in Section 3, including confirming `/api/join` durably persists membership changes (checked independently via `psql`, not just the HTTP response).

**The matching/join figures above are historical component-only results.** Current `creategroups` also includes group creation and the live API suite. See [reports/api-testing.md](reports/api-testing.md) for the merged verification results.

## Study group creation request

`study-mate/src/main/java/com/studymate/request/CreateStudyGroupRequest.java`
defines the incoming JSON fields for group creation. The service operation is
implemented in `StudyGroupService.createStudyGroup` and is exposed through
`POST /api/groups`. Send `Content-Type: application/json` and an `X-API-KEY` header
with the following body. The creator must already exist in the database; there
is currently no student-registration endpoint.

```json
{
  "creatorStudentId": 7,
  "name": "Software Engineering Study Group",
  "course": "COMS W4156",
  "maxMembers": 4,
  "schedules": [
    {"day": "MONDAY", "startTime": "14:00", "endTime": "16:00"}
  ],
  "studyStyles": ["DISCUSSION", "PRACTICE"],
  "meetingMode": "ONLINE"
}
```

`creatorStudentId` is the student's numeric database ID, rather than the string
student identifier stored in a group's membership list. The study-style values
above are examples; a fixed vocabulary is not enforced yet.

The creation service requires an existing creator with a school and student
identifier, a nonblank name and course, capacity of at least two, at least one
valid weekly schedule, and at least one nonblank, unique study style. Schedules
must start before they end and must not overlap on the same day. Adjacent time
slots are allowed. Meeting mode must be `IN_PERSON`, `ONLINE`, or `HYBRID`.

The service copies the creator's school, adds the creator's string student
identifier as the first member, sets the member count to one, and opens
recruitment. It saves a fresh group in a transaction and returns the generated
group ID with the saved group. Repeated valid calls create separate groups.
Invalid values throw `IllegalArgumentException`; a missing creator throws
`NoSuchElementException`. The controller maps these failures to the HTTP responses
below.

| Status | Meaning |
| --- | --- |
| `200` | The group was saved; the response contains the group and its generated ID. |
| `400` | Group values are invalid, or the JSON is missing, malformed, or contains forbidden fields. |
| `401` | The API key is missing, blank, or rejected by the API-key service. |
| `404` | No student exists for the provided creator ID. |
| `415` | The request uses an unsupported content type. |
| `500` | An unexpected server or database failure occurred. |

Service validation and missing-student responses include an `error` message,
for example `{"error":"maxMembers must be at least 2."}`. JSON parsing errors
are handled by Spring and may have a different response body. Spring parses the
body before the controller runs, so malformed JSON can return `400` before the
API key is checked.

The API-key service checks registered students through `existsByApiKey`.
Missing, blank, and unknown keys are rejected. The key check does not verify
that the caller owns the creator's student record.

The request rejects all unrecognized top-level fields, including `groupId`,
`school`, `memberIds`, `numMembers`, `isRecruiting`, and `recruiting`, even when
their value is `null`. These values will be supplied by the server. Rejection is
specific to this request class and does not change JSON handling for existing
endpoints. The request reuses the existing `TimeSlot` representation for schedules.

The JUnit tests in
`study-mate/src/test/java/com/studymate/request/CreateStudyGroupRequestTests.java`
exercise valid JSON, forbidden fields, and misspelled fields using Jackson. They
run without a database. From `study-mate/`, run:

```sh
./mvnw -Dtest=CreateStudyGroupRequestTests test
```

## Group creation tests

Tests use JUnit Jupiter with Mockito for test doubles and Spring MockMvc for
in-memory HTTP request handling. Maven dependencies and the JaCoCo coverage
configuration are in `study-mate/pom.xml`.

| Test file under `study-mate/src/test/java/com/studymate/` | Coverage |
| --- | --- |
| `service/StudyGroupCreationTests.java` | Creator lookup, group defaults, validation, copied input, repeated submissions, and save failures. |
| `controller/StudyGroupCreationApiTests.java` | JSON binding, returned group, missing/blank/rejected keys, `400`/`401`/`404` responses, forbidden fields, and unsupported content types. |
| `request/CreateStudyGroupRequestTests.java` | Accepted JSON fields and rejection of unknown or server-controlled fields. |

Service tests mock student lookup and database access. Controller tests mock the
services while exercising Spring's request parsing and response serialization.
They do not verify real database persistence or real API-key validation.
Tests share setup through `@BeforeEach` and run independently without a required
execution order.

`study-mate/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
selects Mockito's subclass mock maker. The tests mock non-final classes and
interfaces, so no JVM agent attachment is needed.

From `study-mate/`, run all tests and configured checks with:

```sh
./mvnw verify
```

To run only group-creation tests:

```sh
./mvnw -Dtest=StudyGroupCreationTests,StudyGroupCreationApiTests,CreateStudyGroupRequestTests test
```

After `verify`, test results are in `study-mate/target/surefire-reports/` and
the JaCoCo coverage report is at `study-mate/target/site/jacoco/index.html`.
These generated directories are ignored by Git.

## Live HTTP API testing

`study-mate/src/test/java/com/studymate/api/LiveStudyMateApiTests.java` uses JUnit
Jupiter and Java 17's `HttpClient` as the API test runner and HTTP client. It starts
the full Spring Boot application on a random loopback port, sends real HTTP
requests, and uses real services and repositories. No services or repositories
are mocked in this suite.

From `study-mate/`, run:

```sh
./mvnw -Dtest=LiveStudyMateApiTests test
```

The suite also runs as part of `./mvnw verify`. Java 17 and a local TCP port are
required; PostgreSQL, API credentials, and a separately started server are not.
Maven downloads dependencies on the first run as usual.

The test-only `api` profile is configured in
`study-mate/src/test/resources/application-api.properties`. It uses an isolated
in-memory H2 database and binds the server to `127.0.0.1`. `NON_KEYWORDS=DAY,YEAR`
allows the existing entity column names, and Hibernate is configured to fail
startup on schema errors. This does not alter production PostgreSQL settings.

Every test resets the isolated database and commits its prerequisite student
records before sending requests. Preference scenarios also seed prerequisite
preferences through the repository because preference creation is incomplete.
Assertions reload saved data in separate transactions after HTTP responses;
requests are not wrapped in a test transaction that could hide missing commits.
Tests run independently and require no manual student-ID configuration.

| Endpoint | Live scenarios |
| --- | --- |
| `GET /api/health` | Response status and health message. |
| `POST /api/groups` | Success and committed collections; repeated submissions; missing/blank/unknown keys; unknown creator; invalid values, JSON, and server-controlled fields; unsupported content type; rejected requests save no group. |
| `GET /api/{id}` | Retrieve a seeded preference and read it again after an update. |
| `PUT /api/{id}` | Update preferences and confirm values were committed. |
| `GET /api/find` | Discover a newly created group; reject missing students/preferences; return empty results; rank the top three; exclude different school/course/mode, closed, and full groups. |

The suite verifies H2 persistence, not PostgreSQL compatibility. API keys are
validated against registered students; fixtures include a registered key, and
live tests reject unknown keys. Matching uses `GET /api/find?studentId={id}`.
The key check does not establish ownership of the requested student record.

`POST /api/` (initial preference creation) remains outside this acceptance suite
because it does not attach the required student. Joining is now implemented as
`POST /api/join` on main and has its own unit tests and cURL suite described above.
The live suite here focuses on creation, preferences, and matching.

Results are generated under `study-mate/target/surefire-reports/`. A checked-in
run summary and limitations are recorded in [reports/api-testing.md](reports/api-testing.md).

### Leave Study Group (Ziyue Fang), on `ziyue/leave-group` branched from `harry/matching`

```text
./mvnw verify

Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
Checkstyle violations: 0
PMD violations: 0
Branch coverage: 85.9% (79/92); StudyGroupService 100% (10/10)
BUILD SUCCESS
```

`./scripts/api-test-leave.sh` against the local H2 instance: **11/11 passed**, and `./scripts/api-test-find-join.sh` still passes 9/9 on the same instance.

**These numbers are branch-local.** They have not yet been combined with Kevin Chuang's, Hyunwoo Kim's, Ugorji Benjamin Barrah's, or Ziyue Fang's work (Register Student, Create Study Group, Register/Update Study Preference, Leave Study Group respectively) — that merge, and the corresponding re-run of `./mvnw verify` on `main`, is the next step before submission. 
