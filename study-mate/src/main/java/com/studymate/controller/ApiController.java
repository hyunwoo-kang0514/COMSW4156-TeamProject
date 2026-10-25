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
import com.studymate.dto.JoinGroupRequest;
import com.studymate.service.MatchingService;
import com.studymate.service.StudentService;
import com.studymate.service.StudyGroupService;
import com.studymate.service.StudyMateApiService;
import org.springframework.web.bind.annotation.*;

import com.studymate.dto.RegisterStudentRequest;

import java.io.IOException;
import java.util.List;

/**
 * REST entry points for StudyMate. The /find and /join handlers were debugged
 * with AI assistance (Claude Code): fixed an invalid double-@RequestBody binding
 * on /join (moved to JoinGroupRequest + POST), switched /find's studentId to
 * @RequestParam, and corrected the already-a-member case on /join to be
 * idempotent (200 + current group) instead of a 400 error, per the proposal's
 * retry-safety requirement. See README "AI Documentation" section for details.
 */
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

    @PostMapping("/students")
    public ResponseEntity<?> registerStudent(@RequestBody RegisterStudentRequest request) {
        try {
            Student student = new Student();

            student.setStudentId(request.getStudentId());
            student.setName(request.getName());
            student.setEmail(request.getEmail());
            student.setSchool(request.getSchool());
            student.setMajor(request.getMajor());
            student.setYear(request.getYear());

            Student registeredStudent = studentService.registerStudent(student);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(registeredStudent);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
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
     * Find up to the top three study groups compatible with a student's registered preference.
     * @param apiKey caller's API key, validated against a registered student
     * @param studentId id of the student requesting matches
     * @return 200 with the ranked list of compatible groups (possibly empty),
     *         401 if the API key is invalid,
     *         404 if the student does not exist,
     *         400 if the student has not registered a study preference
     */
    @GetMapping("/find")
    public ResponseEntity<?> findCompatibleStudyGroups(
            @RequestHeader("X-API-KEY") final String apiKey,
            @RequestParam final Long studentId
    ) {
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
     * Join a student to a study group. A group no longer needs to be one of the
     * student's current compatible matches in order to join it; the match result
     * is a recommendation, not a requirement.
     * @param apiKey api key
     * @param request body containing the student id and the target group id
     * @return 200 with the updated group on success (also if already a member),
     *         401 if the API key is invalid,
     *         400 if the student or group does not exist, or the group is already full
     */
    @PostMapping("/join")
    public ResponseEntity<?> joinStudyGroup(
            @RequestHeader("X-API-KEY") final String apiKey,
            @RequestBody final JoinGroupRequest request
    ) {
        // Validate whether API key is valid
        if (!studyMateApiService.validateApiKey(apiKey)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
        Student student = studentService.findStudentById(request.getStudentId());
        StudyGroup studyGroup = studyGroupService.findStudyGroupById(request.getGroupId());

        // check either student or study group is valid
        if (student == null || studyGroup == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        // idempotent retry: already a member, return success with the current group as-is
        if (studyGroupService.isMember(student, studyGroup)) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(studyGroup);
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