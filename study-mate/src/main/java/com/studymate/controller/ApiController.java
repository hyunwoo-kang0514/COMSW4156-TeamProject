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

import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.service.MatchingService;
import com.studymate.service.StudentService;
import com.studymate.service.StudyGroupService;
import com.studymate.service.StudyMateApiService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {

    private final PreferenceService preferenceService;
    private final StudyMateApiService studyMateApiService;
    private final StudentService studentService;
    private final MatchingService matchingService;
    private final StudyGroupService studyGroupService;


    @GetMapping("/health")
    public String health() {
        return "StudyMate API is running";
    }


    /**
     * Give a student a specific study preference
     * @param apiKey api key
     * @param preference the preference
     * @return 201/400?
     */
    @PostMapping("/")
    public ResponseEntity<Preference> addStudentStudyPreferences(
            @RequestHeader("X-API-KEY") final String apiKey,
            @RequestBody Preference preference
    ) throws IOException {
        // Validate whether API key is valid
        if(!studyMateApiService.validateApiKey(apiKey)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // Add the preference in the request body to the database
        Preference savedPreference = preferenceService.addStudentStudyPreferences(preference);
        
        return new ResponseEntity<>(savedPreference, HttpStatus.CREATED);

    }

    /**
     * Update a given student's study preference
     * @param id the student id
     * @param preference the preference
     * @return 200/400?
     */
    @PutMapping("/{id}")
    public ResponseEntity<Preference> updateStudentStudyPreferences(
            @RequestHeader("X-API-KEY") final String apiKey,
            @PathVariable Long id, 
            @RequestBody Preference preference
    ) throws IOException {
        // Validate whether API key is valid
        if(!studyMateApiService.validateApiKey(apiKey)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // update the study preference for this student
        Preference savedPreference = preferenceService.updateStudentStudyPreferences(id, preference);
        
        // returns the created study preference
        return new ResponseEntity<>(savedPreference, HttpStatus.OK);

    }


    /**
     * Update a given student's study preference
     * @param apiKey api key
     * @param id the student id
     * @return 200/400?
     */
    @GetMapping("/{id}")
    public ResponseEntity<Preference> getStudentStudyPreferences(
            @RequestHeader("X-API-KEY") final String apiKey,
            @PathVariable Long id
    ) throws IOException {
        // Validate whether API key is valid
        if(!studyMateApiService.validateApiKey(apiKey)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // get the information stored in a student's study preferences
        Preference gottenPreference = preferenceService.getStudentStudyPreferences(id);

        // return the preference according to that student
        return new ResponseEntity<>(gottenPreference, HttpStatus.OK);
    }

    /**
     * Find a compatible study group
     * @param apiKey apikey
     * @param studentId id of the student
     * @return 200/400?
     */
    @GetMapping("/find")
    public ResponseEntity<?> findCompatibleStudyGroups(
            @RequestHeader("X-API-KEY") final String apiKey,
            @RequestBody final Long studentId
    ) throws IOException {
        // Validate whether API key is valid
        if(!studyMateApiService.validateApiKey(apiKey)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
        // Find corresponding student object by ID
        Student student = studentService.findStudentById(studentId);
        // If corresponding student ID DNE
        if (student == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();
        }
        // If student preference is not registered
        if (student.getPreference() == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }
        // find compatible matches
        List<StudyGroup> matches = matchingService.findCompatibleStudyGroups(student);
        // Find compatible matches
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(matches);
    }

    /**
     * Join a student to a study group
     * @param apiKey api key
     * @param studentId id of the student
     * @param groupId id of the group
     * @return 200/400?
     */
    @GetMapping("/join")
    // TO FIX : make a join group request
    public ResponseEntity<?> joinStudyGroup(
            @RequestHeader("X-API-KEY") final String apiKey,
            @RequestBody final Long studentId,
            @RequestBody final Long groupId
    ) {
        // Validate whether API key is valid
        if (!studyMateApiService.validateApiKey(apiKey)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
        Student student = studentService.findStudentById(studentId);
        StudyGroup studyGroup = studyGroupService.findStudyGroupById(groupId);

        // check either student or study group is valid
        if (student == null || studyGroup == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        // check if the student already a member of designated group
        if (studyGroupService.isMember(student, studyGroup)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        // check availability of the group size
        if (!studyGroupService.canJoin(studyGroup)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        studyGroupService.joinGroup(student, studyGroup);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(studyGroup);
    }






}