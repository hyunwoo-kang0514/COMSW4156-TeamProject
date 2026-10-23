package com.studymate.controller;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import com.studymate.domain.Preference;
import com.studymate.service.PreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {

    //MatchingService matchingService = new MatchingService();
    private final PreferenceService preferenceService;
    //StudentService studentService = new StudentService();
    //StudyGroupService studygroupService = new StudyGroupService();


    @GetMapping("/health")
    public String health() {
        return "StudyMate API is running";
    }

    @PostMapping("/")
    public ResponseEntity<Preference> addStudentStudyPreferences(@RequestBody Preference preference) {
        Preference savedPreference = preferenceService.addStudentStudyPreferences(preference);
        return new ResponseEntity<>(savedPreference, HttpStatus.CREATED);

    }

    @PutMapping("/{id}")
    public ResponseEntity<Preference> updateStudentStudyPreferences(@PathVariable Long id, @RequestBody Preference preference) {
        Preference savedPreference = preferenceService.updateStudentStudyPreferences(id, preference);
        return new ResponseEntity<>(savedPreference, HttpStatus.OK);

    }

    @GetMapping("/{id}")
    public ResponseEntity<Preference> getStudentStudyPreferences(@PathVariable Long id){
        Preference gottenPreference = preferenceService.getStudentStudyPreferences(id);
        return new ResponseEntity<>(gottenPreference, HttpStatus.OK);
    }
}