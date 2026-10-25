# COMSW4156-TeamProject
Team project for COMS W4156 Advanced Software Engineering


**Update:** Preferred group size is no longer a strict matching requirement. 
It is now used as a ranking factor, so groups with slightly different sizes may still be recommended.

**Update:** The same-school criterion is added because we assume that we assume that students are finding
study mates where at same school and course

**Update:** A study group no longer needs to be one of the student’s current compatible matches in order for the student to join it. The compatible-match result is treated as a recommendation rather than a strict joining requirement. A student may join 
any valid group as long as the student and group exist, the student is not already a member, and the group has available capacity.

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
below. No creation-service or creation-endpoint tests have been added yet.

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

The existing API-key service is still a placeholder that accepts every supplied
key. This endpoint rejects missing and blank keys, but arbitrary nonblank keys
currently pass. It does not verify that the caller owns the creator's student
record.

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
