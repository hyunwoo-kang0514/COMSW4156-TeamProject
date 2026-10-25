package com.studymate.service;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
 
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
 
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import com.studymate.domain.Preference;
import com.studymate.domain.Student;
import com.studymate.domain.TimeSlot;
import com.studymate.repository.PreferenceRepository;
import com.studymate.repository.StudentRepository;
 
@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {
 
    @Mock
    private PreferenceRepository preferenceRepository;
 
    @Mock
    private StudentRepository studentRepository;
 
    @InjectMocks
    private PreferenceService preferenceService;
 
    private Preference preference(String course, String mode, int groupSize, String style, TimeSlot slot) {
        Preference p = new Preference();
        p.setCourse(course);
        p.setMeetingMode(mode);
        p.setPreferredGroupSize(groupSize);
        p.setStudyStyles(new ArrayList<>(List.of(style)));
        p.setAvailableTimes(new ArrayList<>(List.of(slot)));
        return p;
    }
 
    @Test
    void add_returnsSavedPreference() {
        Preference input = new Preference();
        Preference saved = new Preference();
        when(preferenceRepository.save(input)).thenReturn(saved);
 
        assertSame(saved, preferenceService.addStudentStudyPreferences(input));
        verify(preferenceRepository).save(input);
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
 
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> preferenceService.updateStudentStudyPreferences(99L, new Preference()));
 
        assertEquals("Preference not found for studentId 99", ex.getMessage());
        verify(studentRepository, never()).findById(any());
        verify(preferenceRepository, never()).save(any());
    }
 
    @Test
    void update_studentNotFound_throws() {
        when(preferenceRepository.findById(5L)).thenReturn(Optional.of(new Preference()));
        when(studentRepository.findById(5L)).thenReturn(Optional.empty());
 
        RuntimeException ex = assertThrows(RuntimeException.class,
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
 
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> preferenceService.getStudentStudyPreferences(42L));
 
        assertEquals("Preference not found for studentId 42", ex.getMessage());
    }



}