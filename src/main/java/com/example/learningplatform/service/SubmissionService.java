package com.example.learningplatform.service;

import com.example.learningplatform.dto.GradeSubmissionRequest;
import com.example.learningplatform.dto.SubmissionRequest;
import com.example.learningplatform.dto.SubmissionResponse;
import com.example.learningplatform.entity.Assignment;
import com.example.learningplatform.entity.EnrollmentStatus;
import com.example.learningplatform.entity.Role;
import com.example.learningplatform.entity.Submission;
import com.example.learningplatform.entity.User;
import com.example.learningplatform.exception.DuplicateResourceException;
import com.example.learningplatform.exception.InvalidResourceStateException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.AssignmentRepository;
import com.example.learningplatform.repository.EnrollmentRepository;
import com.example.learningplatform.repository.SubmissionRepository;
import com.example.learningplatform.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public SubmissionResponse create(Integer assignmentId, SubmissionRequest request) {
        Assignment assignment = getAssignment(assignmentId);
        User student = getUser(request.studentId());
        if (student.getRole() != Role.STUDENT) {
            throw new InvalidResourceStateException("Only students can submit assignments");
        }
        Integer courseId = assignment.getLesson().getModule().getCourse().getId();
        if (!enrollmentRepository.existsByStudent_IdAndCourse_IdAndStatus(
                student.getId(), courseId, EnrollmentStatus.ACTIVE)) {
            throw new InvalidResourceStateException(
                    "Active enrollment in the course is required");
        }
        if (LocalDate.now().isAfter(assignment.getDueDate())) {
            throw new InvalidResourceStateException("Assignment deadline has passed");
        }
        if (submissionRepository.existsByAssignment_IdAndStudent_Id(
                assignmentId, request.studentId())) {
            throw new DuplicateResourceException(
                    "Student has already submitted this assignment");
        }
        Submission submission = new Submission(
                assignment, student, LocalDateTime.now(), request.content());
        return toResponse(submissionRepository.save(submission));
    }

    public List<SubmissionResponse> findByAssignmentId(Integer assignmentId) {
        getAssignment(assignmentId);
        return submissionRepository.findAllByAssignment_IdOrderByIdAsc(assignmentId).stream()
                .map(this::toResponse)
                .toList();
    }

    public SubmissionResponse findById(Integer id) {
        return toResponse(getSubmission(id));
    }

    @Transactional
    public SubmissionResponse grade(Integer id, GradeSubmissionRequest request) {
        Submission submission = getSubmission(id);
        Assignment assignment = assignmentRepository.findByIdForUpdate(
                        submission.getAssignment().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment", submission.getAssignment().getId()));
        Integer courseTeacherId = assignment
                .getLesson()
                .getModule()
                .getCourse()
                .getTeacher()
                .getId();
        if (!courseTeacherId.equals(request.teacherId())) {
            throw new InvalidResourceStateException(
                    "Only the course teacher can grade submissions");
        }
        if (request.score() < 0
                || request.score() > assignment.getMaxScore()) {
            throw new InvalidResourceStateException(
                    "Score must be between 0 and assignment max score");
        }
        submission.setScore(request.score());
        submission.setFeedback(request.feedback());
        return toResponse(submission);
    }

    private Assignment getAssignment(Integer id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
    }

    private Submission getSubmission(Integer id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", id));
    }

    private User getUser(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private SubmissionResponse toResponse(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudent().getId(),
                submission.getSubmittedAt(),
                submission.getContent(),
                submission.getScore(),
                submission.getFeedback());
    }
}
