package com.studymate.service;
import com.studymate.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchingServiceTest {

    @Mock
    private StudyGroupService studyGroupService;

    @InjectMocks
    private MatchingService matchingService;

    @Test
    void findCompatibleStudyGroups() {

    }

}
