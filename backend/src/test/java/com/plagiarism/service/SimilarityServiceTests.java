package com.plagiarism.service;

import com.plagiarism.model.Assignment;
import com.plagiarism.model.SimilarityResult;
import com.plagiarism.model.Submission;
import com.plagiarism.repository.AssignmentRepository;
import com.plagiarism.repository.SimilarityResultRepository;
import com.plagiarism.repository.SubmissionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimilarityServiceTests {

    private static final String ASSIGNMENT_ID = "assignment-1";
    private static final String TEACHER_ID = "teacher@example.com";

    private final SubmissionRepository submissionRepository =
            mock(SubmissionRepository.class);
    private final SimilarityResultRepository similarityResultRepository =
            mock(SimilarityResultRepository.class);
    private final AssignmentRepository assignmentRepository =
            mock(AssignmentRepository.class);
    private final List<SimilarityResult> savedResults =
            new ArrayList<>();

    private SimilarityService similarityService;

    @BeforeEach
    void setUp() {
        similarityService = new SimilarityService(
                submissionRepository,
                similarityResultRepository,
                assignmentRepository
        );

        Assignment assignment = new Assignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setTeacherId(TEACHER_ID);
        when(assignmentRepository.findById(ASSIGNMENT_ID))
                .thenReturn(Optional.of(assignment));
        when(similarityResultRepository.findByAssignmentId(ASSIGNMENT_ID))
                .thenAnswer(invocation -> new ArrayList<>(savedResults));

        AtomicInteger id = new AtomicInteger();
        when(similarityResultRepository.save(any(SimilarityResult.class)))
                .thenAnswer(invocation -> {
                    SimilarityResult result = invocation.getArgument(0);
                    result.setId("result-" + id.incrementAndGet());
                    savedResults.add(result);
                    return result;
                });
    }

    @Test
    void scanComparesDifferentStudentsAndReusesSavedPairs() {
        Submission first = submission("submission-1", "student-a");
        Submission second = submission("submission-2", "student-b");
        Submission resubmission = submission("submission-3", "student-a");
        when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID))
                .thenReturn(List.of(first, second, resubmission));

        List<SimilarityResult> firstScan =
                similarityService.scanAssignment(ASSIGNMENT_ID, TEACHER_ID);
        List<SimilarityResult> secondScan =
                similarityService.scanAssignment(ASSIGNMENT_ID, TEACHER_ID);

        assertEquals(2, firstScan.size());
        assertEquals(2, secondScan.size());
        assertTrue(firstScan.stream().allMatch(result ->
                !result.getSubmission1Id().equals(result.getSubmission2Id())));
        assertTrue(firstScan.stream().allMatch(result ->
                (result.getSubmission1Id().equals("submission-1")
                        && result.getSubmission2Id().equals("submission-2"))
                        || (result.getSubmission1Id().equals("submission-2")
                        && result.getSubmission2Id().equals("submission-3"))));
        verify(similarityResultRepository, times(2))
                .save(any(SimilarityResult.class));
    }

    @Test
    void scanIsRestrictedToTheAssignmentOwner() {
        assertThrows(
                AccessDeniedException.class,
                () -> similarityService.scanAssignment(
                        ASSIGNMENT_ID,
                        "another-teacher@example.com"
                )
        );
    }

    @Test
    void assignmentResultsExcludePairsFromTheSameStudent() {
        Submission first = submission("submission-1", "student-a");
        Submission resubmission = submission("submission-2", "student-a");
        Submission otherStudent = submission("submission-3", "student-b");
        when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID))
                .thenReturn(List.of(first, resubmission, otherStudent));

        SimilarityResult sameStudentPair = result(
                "submission-1",
                "submission-2"
        );
        SimilarityResult crossStudentPair = result(
                "submission-1",
                "submission-3"
        );
        when(similarityResultRepository.findByAssignmentId(ASSIGNMENT_ID))
                .thenReturn(List.of(sameStudentPair, crossStudentPair));

        assertEquals(
                List.of(crossStudentPair),
                similarityService.getAssignmentResults(
                        ASSIGNMENT_ID,
                        TEACHER_ID
                )
        );
    }

    @Test
    void compareRejectsSubmissionsFromDifferentAssignments() {
        Submission first = submission("submission-1", "student-a");
        Submission second = submission("submission-2", "student-b");
        second.setAssignmentId("assignment-2");
        when(submissionRepository.findById("submission-1"))
                .thenReturn(Optional.of(first));
        when(submissionRepository.findById("submission-2"))
                .thenReturn(Optional.of(second));

        assertThrows(
                IllegalArgumentException.class,
                () -> similarityService.compareSubmissions(
                        "submission-1",
                        "submission-2",
                        TEACHER_ID
                )
        );
    }

    @Test
    void compareRejectsSubmissionsFromTheSameStudent() {
        Submission first = submission("submission-1", "student-a");
        Submission second = submission("submission-2", "student-a");
        when(submissionRepository.findById("submission-1"))
                .thenReturn(Optional.of(first));
        when(submissionRepository.findById("submission-2"))
                .thenReturn(Optional.of(second));

        assertThrows(
                IllegalArgumentException.class,
                () -> similarityService.compareSubmissions(
                        "submission-1",
                        "submission-2",
                        TEACHER_ID
                )
        );
    }

    private Submission submission(String id, String studentId) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setAssignmentId(ASSIGNMENT_ID);
        submission.setStudentId(studentId);
        submission.setSourceCode(
                "public class Example { public int value() { return 7; } }"
        );
        return submission;
    }

    private SimilarityResult result(String firstSubmissionId, String secondSubmissionId) {
        SimilarityResult result = new SimilarityResult();
        result.setSubmission1Id(firstSubmissionId);
        result.setSubmission2Id(secondSubmissionId);
        return result;
    }
}
