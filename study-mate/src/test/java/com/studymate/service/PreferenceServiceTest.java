package com.studymate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studymate.domain.Preference;
import com.studymate.domain.Student;
import com.studymate.domain.TimeSlot;
import com.studymate.repository.PreferenceRepository;
import com.studymate.repository.StudentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AI-generated (Claude)
@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

  @Mock private PreferenceRepository preferenceRepository;

  @Mock private StudentRepository studentRepository;

  @InjectMocks private PreferenceService preferenceService;

  private Preference preference(
      String course, String mode, int groupSize, String style, TimeSlot slot) {
    Preference p = new Preference();
    p.setCourse(course);
    p.setMeetingMode(mode);
    p.setPreferredGroupSize(groupSize);
    p.setStudyStyles(new ArrayList<>(List.of(style)));
    p.setAvailableTimes(new ArrayList<>(List.of(slot)));
    return p;
  }

  @Test
  void add_newPreference_copiesValuesAndAttachesStudent() {
    TimeSlot slot = mock(TimeSlot.class);
    Preference input = preference("CS101", "ONLINE", 3, "FLASHCARDS", slot);
    input.setId(1L);
    Student student = mock(Student.class);
    when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
    when(preferenceRepository.findById(1L)).thenReturn(Optional.empty());
    when(preferenceRepository.save(any())).thenAnswer(call -> call.getArgument(0));

    Preference result = preferenceService.addStudentStudyPreferences(input);

    assertSame(student, result.getStudent());
    assertEquals("CS101", result.getCourse());
    assertEquals("ONLINE", result.getMeetingMode());
    assertEquals(3, result.getPreferredGroupSize());
    assertEquals(List.of("FLASHCARDS"), result.getStudyStyles());
    assertEquals(List.of(slot), result.getAvailableTimes());
  }

  @Test
  void add_existingPreference_isReplacedInPlace() {
    Preference existing = preference("CS101", "ONLINE", 2, "FLASHCARDS", mock(TimeSlot.class));
    Preference input =
        preference("MATH200", "HYBRID", 4, "PRACTICE_PROBLEMS", mock(TimeSlot.class));
    input.setId(2L);
    when(studentRepository.findById(2L)).thenReturn(Optional.of(mock(Student.class)));
    when(preferenceRepository.findById(2L)).thenReturn(Optional.of(existing));
    when(preferenceRepository.save(existing)).thenReturn(existing);

    Preference result = preferenceService.addStudentStudyPreferences(input);

    assertSame(existing, result);
    assertEquals("MATH200", result.getCourse());
    verify(preferenceRepository).save(existing);
  }

  @Test
  void add_studentNotFound_throws() {
    Preference input = new Preference();
    input.setId(8L);
    when(studentRepository.findById(8L)).thenReturn(Optional.empty());

    NoSuchElementException ex =
        assertThrows(
            NoSuchElementException.class,
            () -> preferenceService.addStudentStudyPreferences(input));

    assertEquals("Student not found for studentId 8", ex.getMessage());
    verify(preferenceRepository, never()).save(any());
  }

  @Test
  void add_missingId_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> preferenceService.addStudentStudyPreferences(new Preference()));

    verify(studentRepository, never()).findById(any());
    verify(preferenceRepository, never()).save(any());
  }

  @Test
  void update_copiesAllFieldsAndAttachesStudent() {
    TimeSlot oldSlot = mock(TimeSlot.class);
    TimeSlot newSlot = mock(TimeSlot.class);
    Preference existing = preference("CS101", "ONLINE", 2, "FLASHCARDS", oldSlot);
    Preference newValues = preference("MATH200", "IN_PERSON", 5, "PRACTICE_PROBLEMS", newSlot);
    Student student = mock(Student.class);
    when(preferenceRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
    when(preferenceRepository.save(existing)).thenReturn(existing);

    Preference result = preferenceService.updateStudentStudyPreferences(1L, newValues);

    assertEquals("MATH200", result.getCourse());
    assertEquals("IN_PERSON", result.getMeetingMode());
    assertEquals(5, result.getPreferredGroupSize());
    assertEquals(List.of("PRACTICE_PROBLEMS"), result.getStudyStyles());
    assertEquals(List.of(newSlot), result.getAvailableTimes());
    assertSame(student, result.getStudent());
    verify(preferenceRepository).save(existing);
  }

  @Test
  void update_keepsOriginalId() {
    Preference existing = new Preference();
    existing.setId(7L);
    Preference newValues = new Preference();
    newValues.setId(999L);
    when(preferenceRepository.findById(7L)).thenReturn(Optional.of(existing));
    when(studentRepository.findById(7L)).thenReturn(Optional.of(mock(Student.class)));
    when(preferenceRepository.save(existing)).thenReturn(existing);

    Preference result = preferenceService.updateStudentStudyPreferences(7L, newValues);

    assertEquals(7L, result.getId());
  }

  @Test
  void update_preferenceNotFound_throws() {
    when(preferenceRepository.findById(99L)).thenReturn(Optional.empty());

    NoSuchElementException ex =
        assertThrows(
            NoSuchElementException.class,
            () -> preferenceService.updateStudentStudyPreferences(99L, new Preference()));

    assertEquals("Preference not found for studentId 99", ex.getMessage());
    verify(studentRepository, never()).findById(any());
    verify(preferenceRepository, never()).save(any());
  }

  @Test
  void update_studentNotFound_throws() {
    when(preferenceRepository.findById(5L)).thenReturn(Optional.of(new Preference()));
    when(studentRepository.findById(5L)).thenReturn(Optional.empty());

    NoSuchElementException ex =
        assertThrows(
            NoSuchElementException.class,
            () -> preferenceService.updateStudentStudyPreferences(5L, new Preference()));

    assertEquals("Student not found for studentId 5", ex.getMessage());
    verify(preferenceRepository, never()).save(any());
  }

  @Test
  void get_returnsPreference() {
    Preference stored = new Preference();
    when(preferenceRepository.findById(1L)).thenReturn(Optional.of(stored));

    assertSame(stored, preferenceService.getStudentStudyPreferences(1L));
  }

  @Test
  void get_notFound_throws() {
    when(preferenceRepository.findById(42L)).thenReturn(Optional.empty());

    NoSuchElementException ex =
        assertThrows(
            NoSuchElementException.class, () -> preferenceService.getStudentStudyPreferences(42L));

    assertEquals("Preference not found for studentId 42", ex.getMessage());
  }
}
