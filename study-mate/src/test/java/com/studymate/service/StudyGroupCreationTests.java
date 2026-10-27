package com.studymate.service;

// AI-generated with OpenAI Codex; see README "AI Documentation".

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.domain.TimeSlot;
import com.studymate.repository.StudyGroupRepository;
import com.studymate.request.CreateStudyGroupRequest;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyGroupCreationTests {

  @Mock private StudyGroupRepository repository;
  @Mock private StudentService studentService;

  private StudyGroupService service;
  private Student creator;
  private CreateStudyGroupRequest request;

  @BeforeEach
  void setUp() {
    service = new StudyGroupService(repository, studentService);
    creator = new Student();
    creator.setId(7L);
    creator.setStudentId("abc123");
    creator.setSchool("Columbia");
    request = new CreateStudyGroupRequest();
    request.setCreatorStudentId(7L);
    request.setName("  Study Buddies  ");
    request.setCourse("  COMS W4156  ");
    request.setMaxMembers(4);
    request.setSchedules(new ArrayList<>(List.of(slot(DayOfWeek.MONDAY, 14, 16))));
    request.setStudyStyles(new ArrayList<>(List.of(" DISCUSSION ", "PRACTICE")));
    request.setMeetingMode("ONLINE");
  }

  @ParameterizedTest
  @ValueSource(strings = {"ONLINE", "IN_PERSON", "HYBRID"})
  void createsGroupWithServerDefaultsAndReturnsRepositoryResult(String mode) {
    request.setMeetingMode(" " + mode + " ");
    when(studentService.findStudentById(7L)).thenReturn(creator);
    when(repository.save(any(StudyGroup.class)))
        .thenAnswer(
            invocation -> {
              StudyGroup group = invocation.getArgument(0);
              assertNull(group.getGroupId());
              group.setGroupId(42L);
              return group;
            });

    StudyGroup group = service.createStudyGroup(request);

    assertEquals(42L, group.getGroupId());
    assertEquals("Study Buddies", group.getName());
    assertEquals("COMS W4156", group.getCourse());
    assertEquals("Columbia", group.getSchool());
    assertEquals(4, group.getMaxMembers());
    assertEquals(List.of("abc123"), group.getMemberIds());
    assertEquals(1, group.getNumMembers());
    assertTrue(group.isRecruiting());
    assertEquals(mode, group.getMeetingMode());
    assertEquals(List.of("DISCUSSION", "PRACTICE"), group.getStudyStyles());
    assertEquals(DayOfWeek.MONDAY, group.getSchedules().get(0).getDay());
    assertEquals(LocalTime.of(14, 0), group.getSchedules().get(0).getStartTime());
    assertEquals(LocalTime.of(16, 0), group.getSchedules().get(0).getEndTime());
    verify(repository).save(group);
  }

  @Test
  void savedGroupDoesNotShareMutableScheduleOrStyleDataWithRequest() {
    when(studentService.findStudentById(7L)).thenReturn(creator);
    when(repository.save(any(StudyGroup.class))).thenAnswer(call -> call.getArgument(0));

    StudyGroup group = service.createStudyGroup(request);
    assertNotSame(request.getSchedules(), group.getSchedules());
    assertNotSame(request.getSchedules().get(0), group.getSchedules().get(0));
    assertNotSame(request.getStudyStyles(), group.getStudyStyles());
    request.getSchedules().get(0).setStartTime(LocalTime.of(15, 0));
    request.getSchedules().clear();
    request.getStudyStyles().clear();

    assertEquals(LocalTime.of(14, 0), group.getSchedules().get(0).getStartTime());
    assertEquals(List.of("DISCUSSION", "PRACTICE"), group.getStudyStyles());
  }

  @Test
  void repeatedRequestsSaveTwoFreshGroups() {
    when(studentService.findStudentById(7L)).thenReturn(creator);
    AtomicLong nextId = new AtomicLong();
    when(repository.save(any(StudyGroup.class)))
        .thenAnswer(
            call -> {
              StudyGroup group = call.getArgument(0);
              assertNull(group.getGroupId());
              group.setGroupId(nextId.incrementAndGet());
              return group;
            });

    StudyGroup first = service.createStudyGroup(request);
    StudyGroup second = service.createStudyGroup(request);

    assertNotSame(first, second);
    assertNotEquals(first.getGroupId(), second.getGroupId());
    assertEquals(1, first.getNumMembers());
    assertEquals(1, second.getNumMembers());
    verify(repository, times(2)).save(any(StudyGroup.class));
  }

  @Test
  void rejectsNullRequestWithoutLookingUpStudentOrSaving() {
    assertThrows(IllegalArgumentException.class, () -> service.createStudyGroup(null));
    verifyNoInteractions(studentService, repository);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(longs = {0, -1})
  void rejectsMissingOrNonpositiveCreatorId(Long id) {
    request.setCreatorStudentId(id);
    assertThrows(IllegalArgumentException.class, () -> service.createStudyGroup(request));
    verifyNoInteractions(studentService, repository);
  }

  @Test
  void rejectsUnknownCreatorWithoutSaving() {
    assertThrows(NoSuchElementException.class, () -> service.createStudyGroup(request));
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"school", "studentId"})
  void rejectsCreatorWithoutRequiredProfileInformation(String field) {
    if (field.equals("school")) {
      creator.setSchool(" ");
    } else {
      creator.setStudentId(null);
    }
    when(studentService.findStudentById(7L)).thenReturn(creator);

    assertThrows(IllegalArgumentException.class, () -> service.createStudyGroup(request));
    verifyNoInteractions(repository);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidRequests")
  void rejectsInvalidGroupWithoutSaving(
      String description, Consumer<CreateStudyGroupRequest> invalidate) {
    invalidate.accept(request);
    when(studentService.findStudentById(7L)).thenReturn(creator);

    IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class, () -> service.createStudyGroup(request), description);

    assertTrue(error.getMessage().length() > 0);
    verify(repository, never()).save(any(StudyGroup.class));
  }

  @Test
  void acceptsMinimumCapacityAdjacentTimesAndSameTimesOnDifferentDays() {
    request.setMaxMembers(2);
    request.setStudyStyles(List.of("DISCUSSION"));
    request.setSchedules(
        List.of(
            slot(DayOfWeek.MONDAY, 14, 16),
            slot(DayOfWeek.MONDAY, 16, 17),
            slot(DayOfWeek.TUESDAY, 14, 16)));
    when(studentService.findStudentById(7L)).thenReturn(creator);
    when(repository.save(any(StudyGroup.class))).thenAnswer(call -> call.getArgument(0));

    StudyGroup group = service.createStudyGroup(request);

    assertEquals(2, group.getMaxMembers());
    assertEquals(3, group.getSchedules().size());
  }

  @Test
  void propagatesDatabaseFailureInsteadOfReportingSuccess() {
    when(studentService.findStudentById(7L)).thenReturn(creator);
    when(repository.save(any(StudyGroup.class)))
        .thenThrow(new IllegalStateException("Save failed"));

    assertThrows(IllegalStateException.class, () -> service.createStudyGroup(request));
  }

  /** Supplies invalid inputs to exercise validation through the public creation operation. */
  private static Stream<Arguments> invalidRequests() {
    return Stream.of(
        invalid("missing name", r -> r.setName(null)),
        invalid("blank name", r -> r.setName(" ")),
        invalid("missing course", r -> r.setCourse(null)),
        invalid("blank course", r -> r.setCourse(" ")),
        invalid("missing capacity", r -> r.setMaxMembers(null)),
        invalid("capacity below two", r -> r.setMaxMembers(1)),
        invalid("missing mode", r -> r.setMeetingMode(null)),
        invalid("blank mode", r -> r.setMeetingMode(" ")),
        invalid("unsupported mode", r -> r.setMeetingMode("ANY")),
        invalid("missing schedules", r -> r.setSchedules(null)),
        invalid("empty schedules", r -> r.setSchedules(List.of())),
        invalid("null schedule", r -> r.setSchedules(Arrays.asList((TimeSlot) null))),
        invalid("missing day", r -> r.setSchedules(List.of(slot(null, 14, 16)))),
        invalid("missing start", r -> r.getSchedules().get(0).setStartTime(null)),
        invalid("missing end", r -> r.getSchedules().get(0).setEndTime(null)),
        invalid("equal times", r -> r.setSchedules(List.of(slot(DayOfWeek.MONDAY, 14, 14)))),
        invalid("reversed times", r -> r.setSchedules(List.of(slot(DayOfWeek.MONDAY, 16, 14)))),
        invalid("overlapping times", r -> r.getSchedules().add(slot(DayOfWeek.MONDAY, 15, 17))),
        invalid(
            "overlap from earlier start",
            r -> r.getSchedules().add(slot(DayOfWeek.MONDAY, 13, 15))),
        invalid("containing interval", r -> r.getSchedules().add(slot(DayOfWeek.MONDAY, 13, 17))),
        invalid("duplicate times", r -> r.getSchedules().add(slot(DayOfWeek.MONDAY, 14, 16))),
        invalid("missing styles", r -> r.setStudyStyles(null)),
        invalid("empty styles", r -> r.setStudyStyles(List.of())),
        invalid("null style", r -> r.setStudyStyles(Arrays.asList((String) null))),
        invalid("blank style", r -> r.setStudyStyles(List.of(" "))),
        invalid(
            "duplicate styles after trimming",
            r -> r.setStudyStyles(List.of("DISCUSSION", " DISCUSSION "))));
  }

  /** Labels each request mutation so a failing parameterized case is identifiable. */
  private static Arguments invalid(String description, Consumer<CreateStudyGroupRequest> mutation) {
    return Arguments.of(description, mutation);
  }

  /** Creates a weekly interval used by schedule-validation scenarios. */
  private static TimeSlot slot(DayOfWeek day, int startHour, int endHour) {
    TimeSlot time = new TimeSlot();
    time.setDay(day);
    time.setStartTime(LocalTime.of(startHour, 0));
    time.setEndTime(LocalTime.of(endHour, 0));
    return time;
  }
}
