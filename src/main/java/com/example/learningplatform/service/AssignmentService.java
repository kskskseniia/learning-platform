package com.example.learningplatform.service;

import com.example.learningplatform.dto.AssignmentRequest;
import com.example.learningplatform.dto.AssignmentResponse;
import com.example.learningplatform.entity.Assignment;
import com.example.learningplatform.entity.Lesson;
import com.example.learningplatform.exception.DuplicateResourceException;
import com.example.learningplatform.exception.InvalidResourceStateException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.AssignmentRepository;
import com.example.learningplatform.repository.LessonRepository;
import com.example.learningplatform.repository.SubmissionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final LessonRepository lessonRepository;
    private final SubmissionRepository submissionRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            LessonRepository lessonRepository,
            SubmissionRepository submissionRepository) {
        this.assignmentRepository = assignmentRepository;
        this.lessonRepository = lessonRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional
    public AssignmentResponse create(Integer lessonId, AssignmentRequest request) {
        Lesson lesson = getLesson(lessonId);
        verifyCourseTeacher(lesson, request.teacherId());
        verifyUniqueTitle(lessonId, request.title(), null);
        Assignment assignment = new Assignment(
                lesson,
                request.title(),
                request.description(),
                request.dueDate(),
                request.maxScore());
        return toResponse(assignmentRepository.save(assignment));
    }

    public List<AssignmentResponse> findByLessonId(Integer lessonId) {
        getLesson(lessonId);
        return assignmentRepository.findAllByLesson_IdOrderByIdAsc(lessonId).stream()
                .map(this::toResponse)
                .toList();
    }

    public AssignmentResponse findById(Integer id) {
        return toResponse(getAssignment(id));
    }

    @Transactional
    public AssignmentResponse update(Integer id, AssignmentRequest request) {
        Assignment assignment = getAssignmentForUpdate(id);
        verifyCourseTeacher(assignment.getLesson(), request.teacherId());
        verifyUniqueTitle(assignment.getLesson().getId(), request.title(), id);
        if (submissionRepository.existsByAssignment_IdAndScoreGreaterThan(
                id, request.maxScore())) {
            throw new InvalidResourceStateException(
                    "Max score cannot be lower than an existing submission score");
        }
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        assignment.setMaxScore(request.maxScore());
        return toResponse(assignment);
    }

    @Transactional
    public void delete(Integer id, Integer teacherId) {
        Assignment assignment = getAssignment(id);
        verifyCourseTeacher(assignment.getLesson(), teacherId);
        assignmentRepository.delete(assignment);
    }

    private Assignment getAssignment(Integer id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
    }

    private Assignment getAssignmentForUpdate(Integer id) {
        return assignmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
    }

    private Lesson getLesson(Integer id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", id));
    }

    private void verifyCourseTeacher(Lesson lesson, Integer teacherId) {
        Integer courseTeacherId = lesson.getModule().getCourse().getTeacher().getId();
        if (!courseTeacherId.equals(teacherId)) {
            throw new InvalidResourceStateException(
                    "Only the course teacher can manage assignments");
        }
    }

    private void verifyUniqueTitle(Integer lessonId, String title, Integer currentId) {
        boolean exists = currentId == null
                ? assignmentRepository.existsByLesson_IdAndTitle(lessonId, title)
                : assignmentRepository.existsByLesson_IdAndTitleAndIdNot(
                        lessonId, title, currentId);
        if (exists) {
            throw new DuplicateResourceException(
                    "Assignment with this title already exists in the lesson");
        }
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getLesson().getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate(),
                assignment.getMaxScore());
    }
}
