package com.studymate.controller;

import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.service.MatchingService;
import com.studymate.service.StudentService;
import com.studymate.service.StudyGroupService;
import com.studymate.service.StudyMateApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StudyMateApiService studyMateApiService;
    private final StudentService studentService;
    private final MatchingService matchingService;
    private final StudyGroupService studyGroupService;

    @Autowired
    public ApiController(final StudyMateApiService studyMateApiService,
                         final StudentService studentService,
                         final MatchingService matchingService,
                         final StudyGroupService studyGroupService
    ) {
        this.studyMateApiService = studyMateApiService;
        this.studentService = studentService;
        this.matchingService = matchingService;
        this.studyGroupService = studyGroupService;
    }

    @GetMapping("/health")
    public String health() {
        return "StudyMate API is running";
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