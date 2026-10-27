package com.plagiarism.service;

import com.plagiarism.exception.ResourceNotFoundException;
import com.plagiarism.model.Assignment;
import com.plagiarism.model.SimilarityResult;
import com.plagiarism.model.Submission;
import com.plagiarism.repository.AssignmentRepository;
import com.plagiarism.repository.SimilarityResultRepository;
import com.plagiarism.repository.SubmissionRepository;
import com.plagiarism.similarity.SimilarityEngine;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SimilarityService {

    private final SubmissionRepository submissionRepository;
    private final SimilarityResultRepository similarityResultRepository;
    private final AssignmentRepository assignmentRepository;
    private final SimilarityEngine similarityEngine;

    public SimilarityService(
            SubmissionRepository submissionRepository,
            SimilarityResultRepository similarityResultRepository,
            AssignmentRepository assignmentRepository) {

        this.submissionRepository =
                submissionRepository;

        this.similarityResultRepository =
                similarityResultRepository;

        this.assignmentRepository =
                assignmentRepository;

        this.similarityEngine =
                new SimilarityEngine();
    }

    public SimilarityResult compareSubmissions(
            String submission1Id,
            String submission2Id,
            String teacherId) {

        Submission submission1 =
                submissionRepository
                        .findById(submission1Id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Submission 1 not found"
                                ));

        Submission submission2 =
                submissionRepository
                        .findById(submission2Id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Submission 2 not found"
                                ));

        validatePair(submission1, submission2);
        requireAssignmentOwner(submission1.getAssignmentId(), teacherId);

        List<SimilarityResult> existingResults =
                similarityResultRepository.findByAssignmentId(
                        submission1.getAssignmentId()
                );
        SimilarityResult existing = findExisting(
                existingResults,
                submission1,
                submission2
        );
        if (existing != null) {
            return existing;
        }

        return createResult(submission1, submission2);
    }

    public List<SimilarityResult> scanAssignment(
            String assignmentId,
            String teacherId) {

        requireAssignmentOwner(assignmentId, teacherId);

        List<Submission> submissions =
                submissionRepository.findByAssignmentId(assignmentId);
        List<SimilarityResult> knownResults =
                new ArrayList<>(
                        similarityResultRepository.findByAssignmentId(
                                assignmentId
                        )
                );
        List<SimilarityResult> scanResults = new ArrayList<>();

        for (int firstIndex = 0; firstIndex < submissions.size(); firstIndex++) {
            Submission first = submissions.get(firstIndex);

            for (int secondIndex = firstIndex + 1;
                 secondIndex < submissions.size();
                 secondIndex++) {

                Submission second = submissions.get(secondIndex);
                if (Objects.equals(first.getStudentId(), second.getStudentId())) {
                    continue;
                }

                SimilarityResult existing =
                        findExisting(knownResults, first, second);
                if (existing != null) {
                    scanResults.add(existing);
                    continue;
                }

                SimilarityResult created = createResult(first, second);
                knownResults.add(created);
                scanResults.add(created);
            }
        }

        scanResults.sort(
                Comparator.comparing(
                        SimilarityResult::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );
        return scanResults;
    }

    public List<SimilarityResult> getAssignmentResults(
            String assignmentId,
            String teacherId) {

        requireAssignmentOwner(assignmentId, teacherId);
        List<SimilarityResult> results =
                new ArrayList<>(
                        similarityResultRepository.findByAssignmentId(
                                assignmentId
                        )
                );
        Map<String, Submission> submissionsById = new HashMap<>();
        for (Submission submission :
                submissionRepository.findByAssignmentId(assignmentId)) {
            submissionsById.put(submission.getId(), submission);
        }
        results.removeIf(result -> {
            Submission first = submissionsById.get(result.getSubmission1Id());
            Submission second = submissionsById.get(result.getSubmission2Id());
            return first == null
                    || second == null
                    || Objects.equals(
                            first.getStudentId(),
                            second.getStudentId()
                    );
        });
        results.sort(
                Comparator.comparing(
                        SimilarityResult::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );
        return results;
    }

    private void validatePair(
            Submission submission1,
            Submission submission2) {

        if (!Objects.equals(
                submission1.getAssignmentId(),
                submission2.getAssignmentId())) {
            throw new IllegalArgumentException(
                    "Only submissions from the same assignment can be compared."
            );
        }

        if (Objects.equals(
                submission1.getStudentId(),
                submission2.getStudentId())) {
            throw new IllegalArgumentException(
                    "Only submissions from different students can be compared."
            );
        }
    }

    private void requireAssignmentOwner(
            String assignmentId,
            String teacherId) {

        Assignment assignment =
                assignmentRepository.findById(assignmentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Assignment not found"
                                ));
        if (!Objects.equals(assignment.getTeacherId(), teacherId)) {
            throw new AccessDeniedException(
                    "Only the assignment owner can view or compare its submissions."
            );
        }
    }

    private SimilarityResult findExisting(
            List<SimilarityResult> results,
            Submission first,
            Submission second) {

        return results.stream()
                .filter(result ->
                        (Objects.equals(
                                result.getSubmission1Id(),
                                first.getId()
                        ) && Objects.equals(
                                result.getSubmission2Id(),
                                second.getId()
                        )) || (Objects.equals(
                                result.getSubmission1Id(),
                                second.getId()
                        ) && Objects.equals(
                                result.getSubmission2Id(),
                                first.getId()
                        ))
                )
                .findFirst()
                .orElse(null);
    }

    private SimilarityResult createResult(
            Submission submission1,
            Submission submission2) {

        double[] scores =
                similarityEngine.calculateSimilarity(
                        submission1.getSourceCode(),
                        submission2.getSourceCode()
                );

        double tokenSimilarity = scores[0];
        double jaccardSimilarity = scores[1];
        double cosineSimilarity = scores[2];
        double structuralSimilarity = scores[3];

        double finalSimilarity =
                similarityEngine.calculateFinalSimilarity(
                        tokenSimilarity,
                        jaccardSimilarity,
                        cosineSimilarity,
                        structuralSimilarity
                );

        String riskLevel =
                similarityEngine.determineRiskLevel(
                        finalSimilarity
                );

        SimilarityResult result =
                new SimilarityResult();

        result.setAssignmentId(
                submission1.getAssignmentId()
        );

        result.setSubmission1Id(
                submission1.getId()
        );

        result.setSubmission2Id(
                submission2.getId()
        );

        result.setTokenSimilarity(
                tokenSimilarity
        );

        result.setJaccardSimilarity(
                jaccardSimilarity
        );

        result.setCosineSimilarity(
                cosineSimilarity
        );

        result.setStructuralSimilarity(
                structuralSimilarity
        );

        result.setFinalSimilarity(
                finalSimilarity
        );

        result.setRiskLevel(
                riskLevel
        );

        result.setCreatedAt(
                LocalDateTime.now()
        );

        return similarityResultRepository.save(result);
    }
}