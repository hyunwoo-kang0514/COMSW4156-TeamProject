package com.studymate.controller;

import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.service.MatchingService;
import com.studymate.service.StudentService;
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

    @Autowired
    public ApiController(final StudyMateApiService studyMateApiService, final StudentService studentService, final MatchingService matchingService) {
        this.studyMateApiService = studyMateApiService;
        this.studentService = studentService;
        this.matchingService = matchingService;
    }

    @GetMapping("/health")
    public String health() {
        return "StudyMate API is running";
    }

    /**
     * Find a compatible study group
     * @param apikey apikey
     * @param studentId id of the student
     * @return 200/400?
     */
    @GetMapping("/find")
    public ResponseEntity<?> findCompatibleStudyGroups(
            @RequestHeader("X-API-KEY") final String apikey,
            @RequestBody final String studentId
    ) throws IOException {
        // Validate whether API key is valid
        if(!studyMateApiService.validateApiKey(apikey)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // Find corresponding student object by ID
        Student student = studentService.findStudentById(studentId);

        // If corresponding student ID DNE, ...
        if (student == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // find compatible matches
        List<StudyGroup> matches = matchingService.findCompatibleStudyGroups(student);


        // Find compatible matches
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .build();
    }



}