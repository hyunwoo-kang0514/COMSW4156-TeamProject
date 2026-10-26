-- AI-generated (Claude Code). See README "AI Documentation" section.
-- Sample fixture data for manual/local API testing against the H2 in-memory DB.
-- Not used by the automated test suite (only loaded when explicitly passed via
-- spring.sql.init.data-locations when running the app locally for curl testing).

INSERT INTO student (id, student_id, name, email, school, major, year, api_key) VALUES
  (1, 'hk3382', 'Harry Kang', 'harry@example.com', 'Columbia', 'Computer Science', 4, 'test-key-1'),
  (2, 'nopref1', 'No Preference Student', 'nopref@example.com', 'Columbia', 'Computer Science', 2, 'test-key-2');

-- Student 1 has a preference registered; student 2 intentionally has none
-- (used to demo the 400 "preference not registered" case on /api/find).
INSERT INTO preference (student_id, course, meeting_mode, preferred_group_size) VALUES
  (1, 'COMS4156', 'in-person', 4);

INSERT INTO preference_available_times (preference_student_id, day, start_time, end_time) VALUES
  (1, 'MONDAY', '14:00:00', '16:00:00');

INSERT INTO preference_study_styles (preference_student_id, study_styles) VALUES
  (1, 'collaborative'),
  (1, 'focused');

-- Group 1 has room and overlaps student 1's schedule/style (used for the find + join-success demo).
-- Group 2 is already full (used for the join "group full" demo).
INSERT INTO study_group (group_id, name, school, course, max_members, num_members, is_recruiting, meeting_mode) VALUES
  (1, 'COMS4156 Study Group', 'Columbia', 'COMS4156', 4, 1, true, 'in-person'),
  (2, 'Full COMS4156 Group', 'Columbia', 'COMS4156', 2, 2, true, 'in-person');

INSERT INTO study_group_member_ids (study_group_group_id, member_ids) VALUES
  (1, 'other-student'),
  (2, 'member-a'),
  (2, 'member-b');

INSERT INTO study_group_schedules (study_group_group_id, day, start_time, end_time) VALUES
  (1, 'MONDAY', '14:00:00', '16:00:00');

INSERT INTO study_group_study_styles (study_group_group_id, study_styles) VALUES
  (1, 'collaborative'),
  (1, 'focused'),
  (2, 'independent');

-- The rows above use explicit ids, which does not advance the identity counters.
-- Restart them past the seeded ids so POST /api/students and POST /api/groups
-- can insert new rows without a primary-key collision.
ALTER TABLE student ALTER COLUMN id RESTART WITH 3;
ALTER TABLE study_group ALTER COLUMN group_id RESTART WITH 3;
