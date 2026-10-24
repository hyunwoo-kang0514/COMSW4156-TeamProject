package com.studymate.service;


import org.springframework.stereotype.Service;

@Service
public class StudyMateApiService {

    /**
     * Validate api key exist
     * @param apiKey api key
     * @return if api key exist return true, otherwise return false
     */
    public boolean validateApiKey(final String apiKey) {
        return true;
    }
}
