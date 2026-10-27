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

Task tracking: see [Section 11](#11-project-management).

---

## 0. Requirements and Local Environment

The project uses:

- Java 17
- Spring Boot 4.1.1 (Spring Web, Spring Data JPA, Hibernate 7.4.5)
- Apache Maven 3.10.0 (via the `mvnw`/`mvnw.cmd` wrapper — no local Maven install required)
- Checkstyle 9.3 via `maven-checkstyle-plugin` 3.6.0 (ruleset: `google_checks.xml`)
- PMD 7.17.0 (via `maven-pmd-plugin` 3.28.0)
- JaCoCo 0.8.14
- JUnit 5, Mockito, AssertJ (test scope)
- PostgreSQL (hosted on Supabase) as the production datastore; H2 for default tests and PostgreSQL for the opt-in API integration suite
- GitHub Actions for Continuous Integration

Local environment used for the verification run documented below:

```text
Java:  OpenJDK 17.0.14 LTS (Amazon Corretto)
Maven: Apache Maven 3.10.0
OS:    macOS 14.5 (aarch64)
```

> **JDK 23 or newer:** those JDKs turn off implicit annotation processing, so Lombok does not run and
> compilation fails with `cannot find symbol` errors on getters/setters. Build with JDK 17–22 (CI
> uses Temurin 17).

The Maven configuration in `study-mate/pom.xml` targets Java 17. All Maven commands below are run from the `study-mate` directory:

```bash
cd study-mate
```

---

## 1. Implementation Status (this iteration / MVP)

All six operations from the proposal are implemented, unit-tested, and API-tested in this iteration:

| Operation | Method & Path | Owner |
|---|---|---|
| Health check | `GET /api/health` | — |
| Register Student | `POST /api/students` | Kevin Chuang |
| Register Study Preference | `POST /api/` | Ugorji Benjamin Barrah |
| Update / Get Study Preference | `PUT /api/{id}`, `GET /api/{id}` | Ugorji Benjamin Barrah |
| Create Study Group | `POST /api/groups` | Hyunwoo Kim |
| Find Compatible Study Groups | `GET /api/find?studentId={id}` | Hyunwoo Kang |
| Join Study Group | `POST /api/join` | Hyunwoo Kang |
| Leave Study Group | `POST /api/leave` | Ziyue Fang |

Known limitations in this iteration:

- `meetingMode` is compared as an exact, case-sensitive string when matching. `POST /api/groups` only accepts `IN_PERSON`, `ONLINE`, or `HYBRID`, while preferences accept any string, so a preference must use one of those three exact values to match groups created through the API.
- The API key proves the caller is *a* registered student. Only `POST /api/leave` also checks that the key belongs to the student named in the request; the other endpoints do not check ownership.
- `POST /api/` and `PUT /api/{id}` are mounted directly under `/api`; they will move to a `/api/preferences` path in the next iteration.

---

## 2. Updates to the Original Proposal

The following business-logic changes were made after the proposal was submitted and are reflected in the current implementation:

1. **Preferred group size is no longer a strict matching requirement.** It is now a ranking factor only — groups with a slightly different size than requested may still be recommended by `findCompatibleStudyGroups`, just with a lower score.
2. **Same-school is now part of the matching criteria.** We assume students want study mates at the same school *and* course, so `findCompatibleStudyGroups` adds this to its filtering/ranking logic.
3. **Joining a group no longer requires the group to be one of the student's current compatible matches.** The match result returned by `findCompatibleStudyGroups` is a recommendation, not a prerequisite. `joinStudyGroup` now only requires that the student and group both exist, the student is not already a member, and the group has available capacity.
4. **Joining a group the student already belongs to is now idempotent.** Per the proposal's retry-safety requirement ("if the student is already a member, the service returns success with the current group record"), a repeat join call returns `200 OK` with the current group state instead of a `400` error.

---

## 3. API Reference

All endpoints are under `/api` and return JSON unless noted. Every endpoint except `GET /api/health` and `POST /api/students` requires an `X-API-KEY` header, which is checked against the `apiKey` of a registered student. You get a key by registering (`POST /api/students`).

**Errors common to every authenticated endpoint:**

| Status | When |
|---|---|
| `401 Unauthorized` | `X-API-KEY` does not match any registered student. For `POST /api/groups`, also when the header is missing or blank. |
| `400 Bad Request` | The `X-API-KEY` header is missing (all endpoints except `POST /api/groups`), the JSON body is malformed, a field has the wrong type (for example a non-numeric path id or an invalid `day`), or a required primitive such as `preferredGroupSize` is missing. Spring returns its default error JSON for these. |
| `415 Unsupported Media Type` | A JSON endpoint is called without `Content-Type: application/json`. |
| `500 Internal Server Error` | Unexpected server or database failure. |

### `GET /api/health`
Liveness check.

| | |
|---|---|
| **Headers** | none |
| **Success** | `200 OK`, plain-text body `StudyMate API is running` |
| **Side effects** | None. |

### `POST /api/students` — Register Student
Registers a new student and generates their API key.

| | |
|---|---|
| **Headers** | `Content-Type: application/json` (no API key) |
| **Body** | `{"studentId": "ab1234", "name": "Alex", "email": "alex@example.com", "school": "Columbia", "major": "Computer Science", "year": 3}` — every string must be non-blank and `year` must be ≥ 1. |
| **Success** | `201 Created` — `{"id": 3, "studentId": "ab1234", "name": "Alex", "email": "...", "school": "Columbia", "major": "Computer Science", "year": 3, "apiKey": "f376fee4-..."}`. `id` is the numeric database id used by every other endpoint; `apiKey` is a random UUID. |
| **Errors** | `400` — a required field is missing/blank or `year` ≤ 0 (plain-text body `Invalid student information`).<br>`409 Conflict` — `studentId` is already registered (plain-text body `Student ID already registered`). |
| **Side effects** | Inserts one `student` row with a unique API key. |
| **Call order** | First call for any new student. Nothing else can be called on their behalf until they have a key. |
| **Idempotent?** | No — a repeat returns `409` and creates nothing. |

### `POST /api/` — Register Study Preference
Registers (or replaces) the study preference of an existing student.

| | |
|---|---|
| **Headers** | `X-API-KEY`, `Content-Type: application/json` |
| **Body** | `{"id": 3, "course": "COMS4156", "meetingMode": "IN_PERSON", "preferredGroupSize": 4, "studyStyles": ["collaborative", "focused"], "availableTimes": [{"day": "MONDAY", "startTime": "14:00", "endTime": "16:00"}]}` — `id` is the student's database `id`; the preference shares that primary key. `day` is a `java.time.DayOfWeek` name. |
| **Success** | `201 Created` — the saved preference (the linked student is not included). |
| **Errors** | `400` — body has no `id` (empty body), plus the common errors.<br>`404 Not Found` — no student with that `id` (empty body). |
| **Side effects** | Inserts or replaces the row in `preference` and its rows in `preference_available_times` and `preference_study_styles`. A student has at most one preference. |
| **Call order** | After `POST /api/students`. Must happen before `GET /api/find` for that student. |
| **Idempotent?** | Yes, in effect — repeating the request leaves the same single preference. |

### `PUT /api/{id}` — Update Study Preference

| | |
|---|---|
| **Headers** | `X-API-KEY`, `Content-Type: application/json` |
| **Path** | `id` — the student's database `id` |
| **Body** | Same shape as `POST /api/`. The body's `id` is ignored; the path decides which preference is updated. |
| **Success** | `200 OK` — the updated preference. |
| **Errors** | `404` — no preference (or no student) exists for `id`, plus the common errors. |
| **Side effects** | Overwrites `course`, `meetingMode`, `preferredGroupSize`, `studyStyles`, and `availableTimes`. The lists are replaced entirely, not merged. |
| **Call order** | After `POST /api/` for that student. |
| **Idempotent?** | Yes. |

### `GET /api/{id}` — Get Study Preference

| | |
|---|---|
| **Headers** | `X-API-KEY` |
| **Path** | `id` — the student's database `id` |
| **Success** | `200 OK` — the stored preference. |
| **Errors** | `404` — no preference exists for `id`, plus the common errors. |
| **Side effects** | None — read-only. |
| **Call order** | After `POST /api/` for that student. |

### `POST /api/groups` — Create Study Group
Creates a study group with the creator as its first member.

| | |
|---|---|
| **Headers** | `X-API-KEY`, `Content-Type: application/json` |
| **Body** | `{"creatorStudentId": 3, "name": "Alex Study Group", "course": "COMS4156", "maxMembers": 3, "schedules": [{"day": "MONDAY", "startTime": "14:00", "endTime": "16:00"}], "studyStyles": ["collaborative"], "meetingMode": "IN_PERSON"}` |
| **Validation** | The creator must exist and have a school and `studentId`. `name` and `course` are non-blank; `maxMembers` ≥ 2; at least one schedule, each starting before it ends and not overlapping another on the same day (back-to-back slots are fine); at least one non-blank, unique study style; `meetingMode` is `IN_PERSON`, `ONLINE`, or `HYBRID`. Any other top-level field — including server-controlled ones like `groupId`, `school`, `memberIds`, `numMembers`, `isRecruiting`/`recruiting`, or a misspelling — is rejected, even when `null`. |
| **Success** | `200 OK` — the saved group, e.g. `{"groupId": 3, "name": "Alex Study Group", "school": "Columbia", "course": "COMS4156", "memberIds": ["ab1234"], "numMembers": 1, "maxMembers": 3, "recruiting": true, ...}`. |
| **Errors** | `400` — a validation rule failed (`{"error": "maxMembers must be at least 2."}`), or an unknown/forbidden field was sent (Spring error JSON).<br>`401` — key missing, blank, or unknown.<br>`404` — no student with `creatorStudentId` (`{"error": "Student not found for ID 999"}`). |
| **Side effects** | Inserts one group in a transaction. The server copies the creator's school, adds the creator's `studentId` as the only member, sets `numMembers` to 1, and opens recruiting. Nothing is saved on any error. |
| **Call order** | The creator must be registered first. |
| **Idempotent?** | No — every valid call creates a separate group. |

Spring parses the body before the controller runs, so malformed JSON returns `400` before the API key is checked.

### `GET /api/find?studentId={id}` — Find Compatible Study Groups
Returns up to three compatible groups, best match first.

| | |
|---|---|
| **Headers** | `X-API-KEY` |
| **Query** | `studentId` — the student's database `id` (required) |
| **Success** | `200 OK` — JSON array of up to 3 groups, or `[]` if nothing matches. |
| **Errors** | `404` — no student with that id.<br>`400` — the student has no preference, or `studentId` is missing; plus the common errors. |
| **Side effects** | None — read-only. |
| **Call order** | The student needs a preference (`POST /api/`). Groups must exist (`POST /api/groups`) for a non-empty result. |
| **Idempotent?** | Yes — recomputed from current data on every call. |

**How matching works** (`MatchingService`): candidates are recruiting groups with the same school as the student and the same `course` and `meetingMode` as the preference; full groups are skipped. Each candidate is scored out of 80: group size (25 if `maxMembers` equals the preferred size, 20 if within ±1), study styles (25 if every preferred style is shared, 20 if at least one is), and weekly time overlap (30 for ≥ 120 min, 25 for ≥ 60 min, 15 for any overlap). The top three by score are returned.

### `POST /api/join` — Join Study Group

| | |
|---|---|
| **Headers** | `X-API-KEY`, `Content-Type: application/json` |
| **Body** | `{"studentId": 1, "groupId": 3}` — `studentId` is the database `id` |
| **Success** | `200 OK` — the group with the student added (or unchanged if they were already a member). |
| **Errors** | `400` — the student or group does not exist, or the group is full (`numMembers >= maxMembers`) or not recruiting; plus the common errors. |
| **Side effects** | Appends the student's `studentId` to `memberIds`, increments `numMembers`, and saves the group. |
| **Call order** | Student and group must exist. `GET /api/find` does **not** need to be called first — its result is only a recommendation. |
| **Idempotent?** | Yes — joining again returns `200` with the unchanged group and never adds the member twice. |

### `POST /api/leave` — Leave Study Group

| | |
|---|---|
| **Headers** | `X-API-KEY`, `Content-Type: application/json` |
| **Body** | `{"studentId": 1, "groupId": 3}` — `studentId` is the database `id` |
| **Success** | `200 OK` — the group with the student removed (or unchanged if they were not a member). |
| **Errors** | `400` — body missing, or `studentId`/`groupId` missing.<br>`401` — unknown key.<br>`403 Forbidden` — the key is valid but belongs to a different student; a student can only remove themselves.<br>`404` — no such student or group. |
| **Check order** | `401` (key) → `400` (body) → `404` (lookup) → `403` (key owner) → membership check. Nothing changes unless every check passes. |
| **Side effects** | Removes every occurrence of the student's `studentId` from `memberIds`, decrements `numMembers` by the number removed (never below 0), and saves in one transaction. The group is kept when its last member leaves, and `isRecruiting` is unchanged. |
| **Call order** | Student and group must exist; for anything to change the student must have joined first. |
| **Idempotent?** | Yes — leaving again returns `200` and does not write to the database, so a retry after a lost response is safe. |

---

## 4. Example Usage Scenarios

These were run against the local H2 instance from [Section 5](#run-locally-without-a-postgres-connection-h2-for-quick-manual-testing) on port 8081. The seed data provides students `1` (`hk3382`, key `test-key-1`, has a preference) and `2` (`nopref1`, key `test-key-2`, no preference), plus group `1` (room left) and group `2` (full). Responses are shortened.

**Scenario 1 — a new student from registration to leaving a group:**
```bash
# 1. Register. Save "id" and "apiKey" from the response.
curl -X POST http://localhost:8081/api/students -H "Content-Type: application/json" \
  -d '{"studentId":"ab1234","name":"Alex","email":"alex@example.com","school":"Columbia","major":"Computer Science","year":3}'
# -> 201 {"id":3,"studentId":"ab1234",...,"apiKey":"f376fee4-..."}
KEY=f376fee4-...   # the apiKey you received

# 2. Register a study preference (id = the student's database id)
curl -X POST http://localhost:8081/api/ -H "X-API-KEY: $KEY" -H "Content-Type: application/json" \
  -d '{"id":3,"course":"COMS4156","meetingMode":"IN_PERSON","preferredGroupSize":3,"studyStyles":["collaborative"],"availableTimes":[{"day":"MONDAY","startTime":"13:00","endTime":"16:00"}]}'
# -> 201 {"id":3,"course":"COMS4156","meetingMode":"IN_PERSON",...}

# 3. Create a group; the creator becomes its first member
curl -X POST http://localhost:8081/api/groups -H "X-API-KEY: $KEY" -H "Content-Type: application/json" \
  -d '{"creatorStudentId":3,"name":"Alex Study Group","course":"COMS4156","maxMembers":3,"schedules":[{"day":"MONDAY","startTime":"14:00","endTime":"16:00"}],"studyStyles":["collaborative"],"meetingMode":"IN_PERSON"}'
# -> 200 {"groupId":3,"memberIds":["ab1234"],"numMembers":1,"recruiting":true,...}

# 4. Find compatible groups
curl "http://localhost:8081/api/find?studentId=3" -H "X-API-KEY: $KEY"
# -> 200 [{"groupId":3,"name":"Alex Study Group",...}]
```

**Scenario 2 — another student joins, retries, and leaves:**
```bash
curl -X POST http://localhost:8081/api/join -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" \
  -d '{"studentId":1,"groupId":3}'
# -> 200 {"groupId":3,"memberIds":["ab1234","hk3382"],"numMembers":2,...}

# Same call again: idempotent, member is not added twice
# -> 200 {"memberIds":["ab1234","hk3382"],"numMembers":2,...}

curl -X POST http://localhost:8081/api/leave -H "X-API-KEY: test-key-1" -H "Content-Type: application/json" \
  -d '{"studentId":1,"groupId":3}'
# -> 200 {"memberIds":["ab1234"],"numMembers":1,...}
```
Group 3 did not have to appear in student 1's `/api/find` results first (Proposal Update #3).

**Scenario 3 — update and read back a preference:**
```bash
curl -X PUT http://localhost:8081/api/3 -H "X-API-KEY: $KEY" -H "Content-Type: application/json" \
  -d '{"course":"COMS4156","meetingMode":"IN_PERSON","preferredGroupSize":4,"studyStyles":["collaborative","focused"],"availableTimes":[{"day":"MONDAY","startTime":"14:00","endTime":"16:00"}]}'
# -> 200, lists replaced with the new values
curl http://localhost:8081/api/3 -H "X-API-KEY: $KEY"
# -> 200, same preference
```

**Contrived bad usage (and why it fails):**

| Call | Result | Why |
|---|---|---|
| `POST /api/students` with the same `studentId` twice | `409` | Student ids are unique; no second row is created. |
| `POST /api/students` with only `{"studentId":"cd5678"}` | `400` | Name, email, school, major, and a positive year are required. |
| `GET /api/find?studentId=1` with `X-API-KEY: bogus` | `401` | The key does not belong to any registered student. |
| `POST /api/join` with no `X-API-KEY` header | `400` | The header is required; Spring rejects the request before the controller runs. |
| `GET /api/find?studentId=99999` | `404` | No student with that id. |
| `GET /api/find?studentId=2` (student 2 has no preference) | `400` | There is nothing to match against; call `POST /api/` first. |
| `POST /api/` without `id` | `400` | The preference's key is the student's id, so it is required. |
| `POST /api/` with `"id": 999`, or `GET`/`PUT /api/999` | `404` | No such student/preference; nothing is saved. |
| `POST /api/groups` with `"maxMembers": 1` | `400` `{"error":"maxMembers must be at least 2."}` | A group needs room for at least one more member. |
| `POST /api/groups` with `"meetingMode": "in-person"` | `400` `{"error":"meetingMode must be IN_PERSON, ONLINE, or HYBRID."}` | Only the three enum-style values are accepted. |
| `POST /api/groups` that includes `"numMembers": 0` | `400` | Server-controlled fields cannot be set by the client. |
| `POST /api/groups` with `"creatorStudentId": 999` | `404` `{"error":"Student not found for ID 999"}` | The creator must be registered. |
| `POST /api/join` into group `2` (2/2 members) | `400` | No capacity left. |
| `POST /api/join` with a `groupId` that does not exist | `400` | Lookup fails before any change. |
| `POST /api/leave` for student 1 using student 3's key | `403` | A student can only remove themselves. |
| `POST /api/leave` with `{"studentId":1}` (no `groupId`) | `400` | Both ids are required; rejected before any lookup. |
| `POST /api/leave` twice in a row | Both `200`; `numMembers` drops once | The second call sees the student is not a member and changes nothing. |

---

## 5. Build, Run, and Test Instructions

**Tech stack:** Java 17, Spring Boot 4.1.1 (Spring Web, Spring Data JPA), Hibernate, PostgreSQL (hosted on Supabase) for the live datastore, H2 (in-memory) for the automated tests and local demos, Maven (via the wrapper) for build/dependency management, Lombok for entity boilerplate.

All configuration needed to build, run, and test is in the repo under `study-mate/`: `pom.xml`, `mvnw`/`mvnw.cmd` and `.mvn/wrapper/`, `src/main/resources/application.properties`, and `src/test/resources/` (`application.properties`, `application-api.properties`, `local-seed.sql`, and the Mockito config). Run every command below from `study-mate/`.

### Build
```bash
cd study-mate
./mvnw compile
```

### Run against the real database (Postgres/Supabase)
The datastore connection is read from environment variables (see `src/main/resources/application.properties`; a local `.env` file is also picked up):
```bash
cd study-mate
DB_URL="jdbc:postgresql://<host>:5432/<db>" \
DB_USERNAME="<username>" \
DB_PASSWORD="<password>" \
./mvnw spring-boot:run
```
The app starts on port `8080` (override with `-Dspring-boot.run.arguments="--server.port=8081"`).

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
This loads `src/test/resources/local-seed.sql` (2 students, 1 preference, 2 groups) so every endpoint can be tried right away; new students and groups get ids starting at 3. `NON_KEYWORDS=YEAR,DAY` is needed because `year` (on `Student`) and `day` (on `TimeSlot`) are reserved words in H2 2.x. H2 is only used for this path and the automated tests, not in production.

### Run the unit and API test suites (push-button)
```bash
cd study-mate
./mvnw test
```
Runs all 187 JUnit Jupiter tests in 13 test classes through Maven Surefire, including the live HTTP API suite. No setup is needed: tests use an in-memory H2 database (`src/test/resources/application.properties`, and `application-api.properties` for the live suite). To run one class: `./mvnw -Dtest=LiveStudyMateApiTests test`.

### Run the cURL API scripts
With the app running locally on H2 (above):
```bash
cd study-mate
BASE_URL=http://localhost:8081 ./scripts/api-test-find-join.sh
BASE_URL=http://localhost:8081 ./scripts/api-test-leave.sh
```
Each prints a `PASS`/`FAIL` line per case and a final tally. The scripts can run in either order against the same instance.

### Full verification (tests + style + static analysis + coverage)
```bash
cd study-mate
./mvnw verify
```
Fails if any test fails, if Checkstyle finds a single violation, or if PMD finds a violation. Writes the reports listed in Sections 6 and 8 under `study-mate/target/`.

---

## 6. Testing

### Unit Testing
- **Frameworks:** JUnit 5 (Jupiter) as the test framework and runner (through Maven Surefire), Mockito for test doubles (`@Mock`/`@InjectMocks` with `MockitoExtension`), AssertJ for fluent assertions, and Spring MockMvc for in-memory HTTP tests of the controller. All come in through `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-jpa-test` in `study-mate/pom.xml`.
- **Configuration:** `study-mate/pom.xml` (dependencies, Surefire, JaCoCo), `study-mate/src/test/resources/application.properties` (H2 test datasource), and `study-mate/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` (selects Mockito's subclass mock maker so no JVM agent is needed).
- **Grouping:** one test class per unit, with shared fixtures built in `@BeforeEach`. Tests are independent and need no particular order.

| Test class (under `study-mate/src/test/java/com/studymate/`) | Unit under test | Tests | Test doubles |
|---|---|---:|---|
| `controller/ApiControllerTest.java` | Find, join, preference, and register endpoints | 24 | All services mocked |
| `controller/LeaveStudyGroupControllerTest.java` | Leave endpoint (401/400/404/403/idempotent/success) | 9 | Services mocked |
| `controller/StudyGroupCreationApiTests.java` | Create-group endpoint via MockMvc (JSON binding, 400/401/404/415) | 22 | Services mocked |
| `request/CreateStudyGroupRequestTests.java` | Create-group request JSON (accepted and rejected fields) | 17 | None (Jackson only) |
| `service/StudyGroupCreationTests.java` | Group creation rules, defaults, and save failures | 40 | Repository and `StudentService` mocked |
| `service/StudyGroupServiceTest.java` | Membership: `isMember`, `canJoin`, `joinGroup`, `leaveGroup` | 15 | Repository mocked |
| `service/PreferenceServiceTest.java` | Preference create/update/get and not-found cases | 10 | Repositories mocked |
| `service/StudentServiceTest.java` | Student lookup and registration validation | 4 | Repository mocked |
| `service/MatchingServiceTest.java` | Scoring and ranking | 3 | `StudyGroupService` mocked |
| `service/StudyMateApiServiceTest.java` | API-key validation | 1 | Repository mocked |
| `RegisterStudentIntegrationTest.java` | Register Student through the full Spring context | 3 | None (H2) |
| `StudyMateApplicationTests.java` | Spring context loads | 1 | None |

### API Testing
Two complementary tools, both in the repo:

1. **JUnit + Java `HttpClient` live suite** — `study-mate/src/test/java/com/studymate/api/LiveStudyMateApiTests.java` (38 tests). It starts the whole application on a random localhost port and sends real HTTP requests. Nothing is mocked: services, repositories, JSON handling, and transactions are real, on an isolated in-memory H2 database (`study-mate/src/test/resources/application-api.properties`). Each test resets the database, seeds the student it needs, and re-reads saved data in a separate transaction to prove the change was committed. It covers health, group creation (success, repeats, every 400/401/404/415 case, and "nothing saved" on rejection), preference create/read/update and their 404s, and matching (ranking, empty results, and every exclusion rule). It runs as part of `./mvnw test`.
2. **cURL scripts** — `study-mate/scripts/api-test-find-join.sh` (9 cases: find/join success, both idempotency cases, 400/401/404) and `study-mate/scripts/api-test-leave.sh` (11 cases: join then leave, idempotent retry, 400/401/403/404). They run against a running server (Section 5) using the fixture data in `study-mate/src/test/resources/local-seed.sql`.

Latest results: [reports/api-testing.md](reports/api-testing.md). All 38 live tests pass, as do 9/9 and 11/11 cURL cases.

### PostgreSQL API integration tests

The opt-in Maven profile `postgres-api` runs
`api/PostgresStudyMateApiIt.java` with Maven Failsafe. It inherits the 38 live
HTTP scenarios from `LiveStudyMateApiTests` and adds a JDBC metadata assertion
that the database really is PostgreSQL, plus a registration/create/join/leave
workflow with direct SQL verification of committed membership.
No services or repositories are mocked. The test configuration is
`study-mate/src/test/resources/application-postgres-api.properties`; the profile
and Failsafe lifecycle are in `study-mate/pom.xml`.

Use a dedicated test database and a role allowed to connect and create schemas.
For example, if Docker is available:

```bash
docker run --rm --name studymate-postgres-tests \
  -e POSTGRES_DB=studymate_test -e POSTGRES_USER=studymate_test \
  -e POSTGRES_PASSWORD=test-password \
  -p 127.0.0.1:5432:5432 postgres:17
```

Once PostgreSQL is ready, from `study-mate/`:

```bash
POSTGRES_TEST_URL=jdbc:postgresql://127.0.0.1:5432/studymate_test \
POSTGRES_TEST_USERNAME=studymate_test \
POSTGRES_TEST_PASSWORD=test-password \
./mvnw -Ppostgres-api verify
```

The test URL and username are required; password defaults to empty for local
trust-authenticated test servers. There is no fallback to H2 or production
`DB_URL`. Do not use the production database. Each run creates a randomly named
`api_test_*` schema and pins both Hibernate and the connection pool to it.
Fixture resets affect only that schema, and `@AfterAll` drops it even if a test
fails. An interrupted/killed JVM may leave a schema requiring manual cleanup.
Hibernate fails startup on DDL errors. No production tables are migrated or
reset. Stop the example container afterward with
`docker stop studymate-postgres-tests`.

`./mvnw verify` without the profile keeps the default H2 behavior. PostgreSQL
integration results are under `study-mate/target/failsafe-reports/`, while the
ordinary test results remain under `study-mate/target/surefire-reports/`.
Do not use `-DskipTests` for a PostgreSQL verification run because it skips
Failsafe too. The recorded PostgreSQL run is in
[reports/postgresql-api-testing.md](reports/postgresql-api-testing.md).

### Test Coverage
- **Tool:** JaCoCo (`jacoco-maven-plugin` 0.8.14), configured in `study-mate/pom.xml`; the report is generated during `./mvnw verify`.
- **Result:** **91.01% branch coverage (162 of 178 branches)** over all main code, above the 55% target for this iteration (and the 80% target for the next one).
- **Checked-in reports:** [reports/jacoco-coverage.xml](reports/jacoco-coverage.xml) and [reports/jacoco-coverage.csv](reports/jacoco-coverage.csv). The HTML report is regenerated at `study-mate/target/site/jacoco/index.html`.

### Bugs found by the tests and fixed
- `POST /api/join` had two `@RequestBody` parameters and failed over real HTTP; it now takes a `JoinGroupRequest`.
- `GET /api/find` read `studentId` from a request body; it is now a query parameter.
- `joinGroup` never saved the new membership; it now calls `save`.
- Joining twice returned `400`; it is now an idempotent `200`.
- `POST /api/` crashed on every call (Hibernate could not save a `@MapsId` entity with a preset id); the service now attaches the student and saves a managed entity.
- Missing students or preferences on `POST /api/`, `PUT /api/{id}`, and `GET /api/{id}` returned `500`; they now return `404`.
- The local H2 seed did not advance the id counters, so registering a student or creating a group on the demo database failed with a primary-key error; the seed now restarts the counters.

---

## 7. Manual End-to-End Verification (against real Postgres/Supabase)

This check was run on the `harry/matching` branch before the other components were merged.
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

- **Tool:** Checkstyle, via `maven-checkstyle-plugin` 3.6.0 (Checkstyle 9.3).
- **Ruleset:** the bundled **`google_checks.xml`** (Google Java Style).
- **Configuration:** `study-mate/pom.xml`. It checks both `src/main` and `src/test` (`includeTestSourceDirectory=true`) and counts every Google-style warning as a violation (`violationSeverity=warning`), so `./mvnw verify` fails unless the whole codebase is clean.
- **Latest report:** [reports/checkstyle-report.txt](reports/checkstyle-report.txt) (summary) and [reports/checkstyle-result.xml](reports/checkstyle-result.xml) (full output). **Result: 0 violations across all 35 Java files.**
- Code was formatted with google-java-format (via `com.spotify.fmt:fmt-maven-plugin`, which is not part of the build). To reformat after editing:
  ```bash
  ./mvnw com.spotify.fmt:fmt-maven-plugin:2.25:format
  ```
- Run the style check on its own:
  ```bash
  ./mvnw checkstyle:check
  # [INFO] You have 0 Checkstyle violations.
  ```

**Self-documenting code:** classes and methods use descriptive names (`findCompatibleStudyGroups`, `isMember`, `canJoin`, `joinGroup`, `leaveGroup`, `validateApiKey`). Every class has a Javadoc header, and public methods document their parameters, return values, and status codes. Inline comments explain the reasoning where it is not obvious, such as the idempotency short-circuits in join/leave and the scoring rules in `MatchingService`.

### Static Analysis
- **Tool:** PMD 7.17.0 (`maven-pmd-plugin` 3.28.0), default ruleset, run during `./mvnw verify`.
- **Latest report:** [reports/pmd.xml](reports/pmd.xml) — **0 violations**. The HTML version is regenerated at `study-mate/target/reports/pmd.html`.

---

## 9. Continuous Integration

**Tool:** GitHub Actions.
**Workflow location:** `.github/workflows/ci.yml`.
**Triggers:** every `push` and `pull_request` targeting `main`.

The workflow starts a PostgreSQL 17 service container and runs `./mvnw -Ppostgres-api verify`. Surefire runs the default 187 tests on H2, then Failsafe runs the PostgreSQL API integration suite against the service container. Both suites and the style/static checks must pass. See the PostgreSQL testing instructions in Section 6; the test database settings are separate from production `DB_*` settings.

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
      POSTGRES_TEST_URL: jdbc:postgresql://localhost:5432/studymate
      POSTGRES_TEST_USERNAME: postgres
      POSTGRES_TEST_PASSWORD: postgres

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
        run: ./mvnw -Ppostgres-api verify
```

Workflow status and logs are visible from the repository's **Actions** tab.

---

## 10. Third-Party Code Disclosure

**Code included in the repository:**

| Code | Location | Source |
|---|---|---|
| Apache Maven Wrapper 3.3.4 scripts (Apache License 2.0) | `study-mate/mvnw`, `study-mate/mvnw.cmd`, `study-mate/.mvn/wrapper/maven-wrapper.properties` | Generated by the Apache Maven Wrapper, https://maven.apache.org/wrapper/. The scripts download Maven 3.10.0 from Maven Central on first use. |

No other third-party source code is copied into the repository. Libraries and tools are declared in `study-mate/pom.xml` and downloaded from Maven Central at build time:

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc` | Web framework, JPA/ORM |
| `org.postgresql:postgresql` | PostgreSQL JDBC driver (production datastore, Supabase) |
| `com.h2database:h2` | In-memory database for tests and local demos |
| `org.projectlombok:lombok` | Generates getters/setters/constructors on entities and DTOs |
| JUnit 5, Mockito, AssertJ, Spring MockMvc (via the Spring Boot test starters) | Testing |
| `org.jacoco:jacoco-maven-plugin` | Coverage reporting |
| `maven-checkstyle-plugin` with its bundled `google_checks.xml` | Style checking |
| `maven-pmd-plugin` | Static analysis |
| `com.spotify.fmt:fmt-maven-plugin` (run from the command line only, not in `pom.xml`) | google-java-format formatting |

---

## 11. Project Management

Tasks for each team member are tracked with **GitHub Issues / GitHub Projects** on the repository (https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject). The owner of each component is listed in the table under [Authors / Team Members](#authors--team-members) and in [Section 1](#1-implementation-status-this-iteration--mvp), and every component was developed on its own branch and merged through a pull request.

**Project board:** [StudyMate — Iteration 1](https://github.com/users/hyunwoo-kang0514/projects/1) (public, linked to this repository, so the Mentor and Instructor can view it without an invite).

| Issue | Task | Owner |
|---|---|---|
| [#10](https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject/issues/10) | Project setup: Spring Boot skeleton, PostgreSQL/Supabase connection, H2 test config, CI | @hyunwoo-kang0514 |
| [#11](https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject/issues/11) | Find Compatible Study Groups (`GET /api/find`) | @hyunwoo-kang0514 |
| [#12](https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject/issues/12) | Register Student (`POST /api/students`) | @kchua220 |
| [#13](https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject/issues/13) | Join Study Group (`POST /api/join`) | @hyunwoo-kang0514 |
| [#14](https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject/issues/14) | Register / Update / Get Study Preference | @ubb2000 |
| [#15](https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject/issues/15) | Create Study Group (`POST /api/groups`) | @hywkim-brian |
| [#16](https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject/issues/16) | Leave Study Group (`POST /api/leave`) | @Tobbs2005 |
| [#17](https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject/issues/17) | Iteration 1 cleanup: Checkstyle-clean codebase, preference 404 fixes, refreshed reports | @Tobbs2005 |
| [#18](https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject/issues/18) | Document AI usage for every component | @hywkim-brian, @kchua220, @ubb2000 |
| [#19](https://github.com/hyunwoo-kang0514/COMSW4156-TeamProject/issues/19) | Tag the first-iteration release on `main` | Team |

---

## 12. AI Documentation

**Tool used:** Claude Code (Anthropic), via its CLI/terminal integration (model: Claude Opus/Sonnet 5 family, accessed through Hyunwoo Kang's Claude account).

**How it was used (this component — Find Compatible Study Groups / Join Study Group):**
- Generated the full unit test suite in `study-mate/src/test/java/com/studymate/controller/ApiControllerTest.java` from scratch, based on the existing `MatchingServiceTest`/`StudyGroupServiceTest` Mockito patterns already in the repo.
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

**Claude attribution:** AI-authored or AI-debugged portions described in this Claude section are marked inline with a comment (e.g., `// AI-generated (Claude Code)` / `// AI-fixed (Claude Code)`) at the top of each affected file or at the specific changed lines, including test code (`ApiControllerTest.java`, the updated `joinGroup` test in `StudyGroupServiceTest.java`), so changes are traceable even after manual review/edits.

**Estimated token usage (this component, this session):** ~400,000–600,000 tokens (combined input + output, including tool/command output consumed as context). This is a rough self-reported estimate based on session length and complexity, not an exact billing figure — for the precise number, pull actual usage from the Claude Code session cost report (`/cost`) or the Anthropic Console usage dashboard tied to the account used.

### Leave Study Group component (Ziyue Fang)

**Tool used:** Claude (Anthropic), Cowork mode, model Claude Opus 5.5, via Ziyue Fang's Claude account.

**What it generated (all reviewed by Ziyue Fang and marked inline with `AI-generated` comments, following the team convention):**
- `POST /api/leave` handler in `ApiController.java` and `StudyGroupService.leaveGroup()`.
- `dto/LeaveGroupRequest.java`.
- `controller/LeaveStudyGroupControllerTest.java` and the `leaveGroup` tests at the end of `service/StudyGroupServiceTest.java`.
- `scripts/api-test-leave.sh`.
- The `/api/leave` parts of this README, plus the `NON_KEYWORDS=YEAR,DAY` fix to the local H2 run command (found while running the API tests).

**Representative prompt:** "Implement my part (Leave Study Group): entity/repository/service logic, the endpoint, validation, failure cases, idempotency, unit tests with mocks, API tests, and the README section."

### Group creation and API testing (OpenAI Codex)

OpenAI Codex was used through the Codex application to generate the group-creation
request, the creation endpoint, the service's creation and validation methods,
and the request/service/controller tests. It also generated the live HTTP API
suite, PostgreSQL integration configuration, and related documentation, and
assisted with merge resolution and verification. Existing teammate implementations
and their attribution are retained.

All five test classes generated in this collaboration carry Codex attribution:
`CreateStudyGroupRequestTests`, `StudyGroupCreationTests`,
`StudyGroupCreationApiTests`, `LiveStudyMateApiTests`, and
`PostgresStudyMateApiIt`.

For the instructor-directed evaluation, production-code labels are intentionally
partial: 68.9% of the nonblank implementation lines across the six
Codex-generated production methods carry method-level attribution. Whole methods
are selected, with approximately 30% left unlabeled. This percentage describes
those methods, not the entire repository. Absence of an inline label does not
assert human authorship. No human-fixed attribution has been added.

Representative prompts from this collaboration:
- "Define a separate creation-request class ... Explicitly reject attempts to supply server-controlled fields."
- "ok implement it and explain (do not implement the tests yet)"
- "do the next step in APicontroller"
- "create test cases"
- "can you do the API testing part now"
- "add tests configured for PostgreSQL and commit"

### PostgreSQL API test configuration (OpenAI Codex)

OpenAI Codex was used through the Codex application to add
`PostgresStudyMateApiIt.java`, the PostgreSQL test profile, Maven Failsafe/CI
configuration, and their documentation. The Java test file is marked
AI-generated. Prompt: "add tests configured for PostgreSQL and commit".
This session's token total is not exposed to this task; it has not been
estimated or invented and still needs to be supplied from account/session
usage records for the assignment's team-wide accounting.
### Create Study Group component (Hyunwoo Kim)

**Tool used:** None reported. No AI tool usage was reported for this component in Iteration 1.

**AI-generated or AI-debugged code:** None reported. None of this component's files carry an `AI-generated` / `AI-fixed` marker. Files in this component: `POST /api/groups` handler in `ApiController.java`, `StudyGroupService` group-creation logic, `request/CreateStudyGroupRequest.java`, `request/CreateStudyGroupRequestTests.java`, `service/StudyGroupCreationTests.java`, `controller/StudyGroupCreationApiTests.java`, `api/LiveStudyMateApiTests.java`, and the `/api/groups` parts of this README.

**Representative prompts:** N/A.

**Estimated token usage:** 0 reported.

### Register Student component (Kevin Chuang)

**Tool used:** None reported. No AI tool usage was reported for this component in Iteration 1.

**AI-generated or AI-debugged code:** None reported. None of this component's files carry an `AI-generated` / `AI-fixed` marker. Files in this component: `POST /api/students` handler in `ApiController.java`, `StudentService` registration logic, `StudentRepository`, `dto/RegisterStudentRequest.java`, `dto/RegisterStudentResponse.java`, `service/StudentServiceTest.java`, `RegisterStudentIntegrationTest.java`, and the `/api/students` parts of this README.

**Representative prompts:** N/A.

**Estimated token usage:** 0 reported.

### Study Preference component (Ugo Barrah)

**Tool used:** Claude (Anthropic), via the Claude app (model: Claude Opus 5.5), accessed through Ugo Barrah's Claude account.

**How it was used (this component — Add / Update / Get Study Preferences):**

* Wrote most of the unit tests for this component, including the preference-endpoint tests (`POST /api/`, `PUT /api/{id}`, `GET /api/{id}`) in `study-mate/src/test/java/com/studymate/controller/ApiControllerTest.java`, following the existing Mockito Arrange/Act/Assert patterns

* Helped debug errors in `PreferenceService.java`, `ApiController.java`, and the `Preference` entity found while building and manually testing the endpoints:
   * **Spring wiring and repository typing:** the controller and service created their dependencies with `new` instead of constructor injection, a `final` field had no constructor to set it, and `PreferenceRepository` extended a raw `JpaRepository` (no `<Preference, Long>`), so `save()` returned `Object`. Fixed with constructor injection (`@AllArgsConstructor`) and the correct generics.
   * **`Optional` handling in update/get:** `findById()` returns `Optional<Preference>`, which was assigned directly to a `Preference`. Fixed with `.orElseThrow(...)`. The update also stopped modifying the primary key, which would have caused a Hibernate "identifier altered" error.
   * **Linking `Preference` to `Student` with a shared primary key:** `Student`'s `mappedBy = "student"` pointed to a field that didn't exist, and the relationship annotations were attached to the wrong field (`school`). Fixed with a `@OneToOne` + `@MapsId` `student` field, `@JsonIgnore` to prevent JSON infinite recursion, and setting the `Student` in `addStudentStudyPreferences()` before saving.
   * **CI `./mvnw verify` failures:** the app ran locally, but CI failed on PMD violations (unused and duplicate imports in `ApiController.java`, `PreferenceService.java`, and `Preference.java`). Fixed by removing them and running `./mvnw clean verify` locally before pushing.
* Helped draft the Javadoc comments for the preference service methods and the API Reference entries for the three preference endpoints in this README.

**Representative prompts used:**

* "What is wrong with my project from seeing this error code?" (with the Maven / Spring stack trace pasted)
* "Add tests in this format for the other 3 API controller tests."

All AI-authored or AI-debugged code in this component is marked inline with the same comment markers used elsewhere in this README (e.g., `// AI-generated (Claude)` / `// AI-fixed (Claude)`), at the top of each affected file or at the specific changed lines, including test code in `ApiControllerTest.java`, so changes remain traceable after manual review and edits.

**Estimated token usage (this component):** ~150,000–250,000 tokens (combined input + output of the conversation). This is a rough self-reported estimate based on conversation length, not an exact billing figure. Actual processed tokens are higher, because earlier conversation context is re-read on each turn.

**Representative prompts:** N/A.

**Estimated token usage:** 0 reported.

### Token usage summary

| Member | Component | Tool | Total tokens |
|---|---|---|---|
| Hyunwoo Kang | Find / Join Study Group | Claude Code | ~400,000–600,000 (estimate) |
| Ziyue Fang | Leave Study Group | Claude (Cowork, Opus 5.5) | Not recorded |
| Hyunwoo Kim | Create Study Group | Codex | ~100,000-200,000 |
| Kevin Chuang | Register Student | None reported | 0 |
| Ugo Barrah | Study Preference | Claude Code | ~150,000 |

---

## 13. Final Verification

Run from `study-mate/` with every component merged:

```text
./mvnw verify

Tests run: 187, Failures: 0, Errors: 0, Skipped: 0
You have 0 Checkstyle violations.
PMD violations: 0
Branch coverage: 91.01% (162/178)
BUILD SUCCESS
```

cURL scripts against the local H2 instance: `api-test-find-join.sh` 9/9 passed, `api-test-leave.sh` 11/11 passed. The checked-in reports are in [`reports/`](reports/).
