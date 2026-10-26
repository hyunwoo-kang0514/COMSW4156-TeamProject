# Live HTTP API test report

Run date: October 25, 2026 (America/New_York).

The API suite uses JUnit Jupiter and Java 17's HTTP client to send requests to
the complete Spring Boot application on an automatically selected localhost
port. Services, repositories, request parsing, response serialization, and
transactions are real. The database is isolated, in-memory H2; no production
database or credentials are used.

## Reproduce

From `study-mate/`, with Java 17:

```sh
./mvnw -Dtest=LiveStudyMateApiTests test
./mvnw verify
```

The first command runs the live API suite. The second also runs unit and
MockMvc tests, generates JaCoCo reports, and executes the configured static
checks. The API suite starts and stops its own server and seeds prerequisite
records automatically. Test code is in
`study-mate/src/test/java/com/studymate/api/LiveStudyMateApiTests.java`;
configuration is in `study-mate/src/test/resources/application-api.properties`.

## Results

| Suite | Cases | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Live HTTP API | 36 | 0 | 0 | 0 |
| Group creation service | 40 | 0 | 0 | 0 |
| MockMvc creation endpoint | 22 | 0 | 0 | 0 |
| Creation request JSON | 17 | 0 | 0 | 0 |
| Application startup | 1 | 0 | 0 | 0 |
| Incoming matching service | 3 | 0 | 0 | 0 |
| Incoming group service | 10 | 0 | 0 | 0 |
| Incoming API-key service | 1 | 0 | 0 | 0 |
| Incoming controller | 11 | 0 | 0 | 0 |
| Total | 141 | 0 | 0 | 0 |

Maven `verify`: passed. Existing application Checkstyle warnings remain;
passing the configured build is not evidence of a clean whole-project style
report. The new live API test file was separately checked against Google
Checkstyle rules; see [api-checkstyle.txt](api-checkstyle.txt).

Overall JaCoCo branch coverage: **121 of 130 branches, 93.08%**. This exceeds
the first-iteration 55% requirement; it also exceeds the second-iteration 80% coverage threshold. The generated XML is included in
[jacoco-coverage.xml](jacoco-coverage.xml). Re-running `verify` generates HTML
at `study-mate/target/site/jacoco/index.html` and test results under
`study-mate/target/surefire-reports/`.

## Live scenarios

- Health returns `200` and its expected message.
- Group creation returns `200` and a generated ID. A separate database
  transaction reloads the group and verifies membership, count, school,
  capacity, recruiting status, mode, schedules, and styles.
- Repeating a valid creation request commits two distinct groups.
- Missing, blank, or unregistered creation keys return `401` without saving a group.
- A nonexistent creator returns `404` without saving a group.
- Invalid group fields, malformed JSON, and forbidden fields return `400`
  without saving a group.
- An unsupported request content type returns `415` without saving a group.
- Existing preferences can be read, updated, read again over HTTP, and
  independently verified in the database.
- Matching returns a newly created group, ranks the top three candidates,
  and excludes different courses, modes, schools, closed groups, and full
  groups.
- Matching returns an empty list when no groups exist, `404` for a missing
  student, and `400` for a student without preferences.

## Scope and limitations

H2 exercises actual persistence but does not establish PostgreSQL compatibility.
The test profile allows the existing `DAY` and `YEAR` column names and fails
startup on schema-generation errors. Production configuration was not changed.

The merged API-key validator checks registered student keys. Fixtures now seed
an API key, and the live suite verifies that unknown keys return `401` without
saving a group. Matching requests use `/api/find?studentId={id}` as required
by the merged controller. Key validation does not establish student ownership.

Student and preference prerequisites are inserted directly into the test
database and committed before requests. Student registration is absent, and
initial preference creation (`POST /api/`) remains incomplete because it does
not attach the required student. Joining is now implemented as `POST /api/join`
and has its own incoming unit tests and cURL script. The live HTTP suite in this
report focuses on creation, preferences, and matching.
