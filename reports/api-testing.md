# Live HTTP API test report

Run date: October 26, 2026 (America/New_York).

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
| Live HTTP API (`api.LiveStudyMateApiTests`) | 38 | 0 | 0 | 0 |
| Group creation service (`service.StudyGroupCreationTests`) | 40 | 0 | 0 | 0 |
| Find/join/preference/register controller (`controller.ApiControllerTest`) | 24 | 0 | 0 | 0 |
| MockMvc creation endpoint (`controller.StudyGroupCreationApiTests`) | 22 | 0 | 0 | 0 |
| Creation request JSON (`request.CreateStudyGroupRequestTests`) | 17 | 0 | 0 | 0 |
| Group membership service (`service.StudyGroupServiceTest`) | 15 | 0 | 0 | 0 |
| Preference service (`service.PreferenceServiceTest`) | 10 | 0 | 0 | 0 |
| Leave controller (`controller.LeaveStudyGroupControllerTest`) | 9 | 0 | 0 | 0 |
| Student service (`service.StudentServiceTest`) | 4 | 0 | 0 | 0 |
| Matching service (`service.MatchingServiceTest`) | 3 | 0 | 0 | 0 |
| Register student integration (`RegisterStudentIntegrationTest`) | 3 | 0 | 0 | 0 |
| API-key service (`service.StudyMateApiServiceTest`) | 1 | 0 | 0 | 0 |
| Application startup (`StudyMateApplicationTests`) | 1 | 0 | 0 | 0 |
| Total | 187 | 0 | 0 | 0 |

Maven `verify`: passed. Checkstyle (Google `google_checks.xml`, main and test
sources, every warning counted as a violation) reports 0 violations; see
[checkstyle-report.txt](checkstyle-report.txt) and
[checkstyle-result.xml](checkstyle-result.xml). PMD reports 0 violations; see
[pmd.xml](pmd.xml).

Overall JaCoCo branch coverage: **162 of 178 branches, 91.01%**. This exceeds
the first-iteration 55% requirement and the second-iteration 80% threshold. The
generated XML and CSV are included in [jacoco-coverage.xml](jacoco-coverage.xml)
and [jacoco-coverage.csv](jacoco-coverage.csv). Re-running `verify` generates HTML
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
- A preference can be created with `POST /api/` and is verified in the database.
- Existing preferences can be read, updated, read again over HTTP, and
  independently verified in the database.
- `POST /api/`, `GET /api/{id}`, and `PUT /api/{id}` return `404` for an
  unknown student without saving a preference.
- Matching returns a newly created group, ranks the top three candidates,
  and excludes different courses, modes, schools, closed groups, and full
  groups.
- Matching returns an empty list when no groups exist, `404` for a missing
  student, and `400` for a student without preferences.

## Scope and limitations

The default H2 suite exercises actual persistence. A separate opt-in PostgreSQL
suite now runs the shared scenarios against PostgreSQL; see
[postgresql-api-testing.md](postgresql-api-testing.md) for its configuration and
verified results. The 187-test table above remains the default suite count.
The test profile allows the existing `DAY` and `YEAR` column names and fails
startup on schema-generation errors. Production configuration was not changed.

The merged API-key validator checks registered student keys. Fixtures now seed
an API key, and the live suite verifies that unknown keys return `401` without
saving a group. Matching requests use `/api/find?studentId={id}` as required
by the merged controller. Key validation does not establish student ownership.

Student and preference prerequisites are inserted directly into the test
database and committed before requests so each scenario is independent.
Preference creation (`POST /api/`) now attaches the student named by the
body's `id` and is covered by its own scenario. Student registration has its
own integration test (`RegisterStudentIntegrationTest`). Joining is now implemented as `POST /api/join`
and has its own incoming unit tests and cURL script. The live HTTP suite in this
report focuses on creation, preferences, and matching.
