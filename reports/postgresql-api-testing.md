# PostgreSQL API integration verification

Run date: October 26, 2026 (America/New_York).

## Configuration

- Test class: `study-mate/src/test/java/com/studymate/api/PostgresStudyMateApiIt.java`.
- Spring profile: `postgres-api`, with `inheritProfiles=false` so the inherited
  live suite's H2 `api` profile is not activated.
- Datasource settings: `study-mate/src/test/resources/application-postgres-api.properties`.
- Runner: Maven Failsafe 3.5.6, enabled by the `postgres-api` Maven profile.
- Java: OpenJDK 17.0.20.1.
- Actual database: PostgreSQL 17.6, a temporary local cluster bound to loopback.
  Production PostgreSQL/Supabase was not accessed or changed.

The local verification server used testing binaries from
[Zonky's PostgreSQL binary distribution](https://github.com/zonkyio/embedded-postgres-binaries),
downloaded from Maven Central into `/private/tmp`. They are not vendored or
required dependencies of the project. CI instead uses its PostgreSQL 17 service
container. Any suitable PostgreSQL test instance can be supplied using the
environment variables below.

## Reproduce

From `study-mate/`, against an existing, dedicated PostgreSQL test database:

```sh
POSTGRES_TEST_URL=jdbc:postgresql://127.0.0.1:5432/studymate_test \
POSTGRES_TEST_USERNAME=studymate_test \
POSTGRES_TEST_PASSWORD=test-password \
./mvnw -Ppostgres-api verify
```

The role needs permission to create schemas. Missing test settings, a non-PG
URL, or an unavailable database fail the run; there is no H2 fallback. Default
`./mvnw verify` still runs the H2-based suite without needing PostgreSQL.

Each run creates a generated `api_test_*` schema, selects it in both Hikari and
Hibernate, and removes only that schema afterward. The test does not recreate
or reset the database itself. The local verification independently queried
`information_schema.schemata` afterward and found **zero remaining test schemas**.

## Results

| Stage | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Default Surefire suite | 187 | 0 | 0 | 0 |
| PostgreSQL Failsafe suite | 40 | 0 | 0 | 0 |
| Total | 227 | 0 | 0 | 0 |

- Maven: **BUILD SUCCESS**.
- Checkstyle: **0 violations**, including main and test sources.
- PMD: **0 violations**.
- Main-code branch coverage: **162/178, 91.01%**.

The PostgreSQL suite inherits all 38 live HTTP scenarios covering health,
group creation, JSON and field validation, API keys, preferences, matching,
duplicate submissions, and committed collection data. No services or
repositories are mocked.

Two additional checks:

1. JDBC metadata must report `PostgreSQL`, and the connection must use the
   generated test schema.
2. A real HTTP workflow registers two students, creates a group, joins and
   retries joining, rejects leaving on another student's behalf, leaves and
   retries leaving. Separate direct SQL queries verify that only the owner
   remains in the committed membership table with `num_members = 1`.

Generated detailed results are in `study-mate/target/failsafe-reports/` and
`study-mate/target/surefire-reports/`. They are intentionally not copied here
because their XML includes machine-specific runtime properties.

## Limitations

This proves the tested scenarios against a clean PostgreSQL schema. It does
not test migrations or pre-existing Supabase constraints and data, concurrency,
or all malformed preference inputs. An interrupted JVM can leave its generated
schema behind; a dedicated test database remains required. The GitHub Actions
configuration has been updated, but the hosted workflow has not been run as
part of this local verification.
