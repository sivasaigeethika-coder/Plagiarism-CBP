package com.plagiarism.controller;

import com.plagiarism.model.SimilarityResult;
import com.plagiarism.service.SimilarityService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/similarity")
public class SimilarityController {

    private final SimilarityService similarityService;

    public SimilarityController(
            SimilarityService similarityService) {

        this.similarityService =
                similarityService;
    }

    @PostMapping("/compare")
    public ResponseEntity<SimilarityResult>
    compareSubmissions(
            @RequestParam String submission1Id,
            @RequestParam String submission2Id,
            Authentication authentication) {

        return ResponseEntity.ok(
                similarityService.compareSubmissions(
                        submission1Id,
                        submission2Id,
                        authentication.getName()
                )
        );
    }

    @PostMapping("/assignment/{assignmentId}/scan")
    public ResponseEntity<List<SimilarityResult>>
    scanAssignment(
            @PathVariable String assignmentId,
            Authentication authentication) {

        return ResponseEntity.ok(
                similarityService.scanAssignment(
                        assignmentId,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<List<SimilarityResult>>
    getAssignmentResults(
            @PathVariable String assignmentId,
            Authentication authentication) {

        return ResponseEntity.ok(
                similarityService
                        .getAssignmentResults(
                                assignmentId,
                                authentication.getName()
                        )
        );
    }
}