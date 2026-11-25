package com.example.learningplatform.service;

import com.example.learningplatform.dto.CourseReviewRequest;
import com.example.learningplatform.dto.CourseReviewResponse;
import com.example.learningplatform.dto.CourseReviewUpdateRequest;
import com.example.learningplatform.entity.Course;
import com.example.learningplatform.entity.CourseReview;
import com.example.learningplatform.entity.Role;
import com.example.learningplatform.entity.User;
import com.example.learningplatform.exception.DuplicateResourceException;
import com.example.learningplatform.exception.InvalidResourceStateException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.CourseRepository;
import com.example.learningplatform.repository.CourseReviewRepository;
import com.example.learningplatform.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseReviewService {

    private final CourseReviewRepository courseReviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseReviewService(
            CourseReviewRepository courseReviewRepository,
            CourseRepository courseRepository,
            UserRepository userRepository) {
        this.courseReviewRepository = courseReviewRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CourseReviewResponse create(CourseReviewRequest request) {
        User student = userRepository.findByIdForUpdate(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.studentId()));
        if (student.getRole() != Role.STUDENT) {
            throw new InvalidResourceStateException("Course review author must have STUDENT role");
        }
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.courseId()));
        String comment = normalizeComment(request.comment());
        ensureUniqueComment(student.getId(), course.getId(), comment, null);
        CourseReview review = new CourseReview(
                course,
                student,
                request.rating(),
                comment,
                LocalDateTime.now());
        return toResponse(courseReviewRepository.save(review));
    }

    public List<CourseReviewResponse> findAll() {
        return courseReviewRepository.findAll(Sort.by("id")).stream()
                .map(this::toResponse)
                .toList();
    }

    public CourseReviewResponse findById(Integer id) {
        return toResponse(getCourseReview(id));
    }

    @Transactional
    public CourseReviewResponse update(Integer id, CourseReviewUpdateRequest request) {
        CourseReview review = getCourseReview(id);
        String comment = normalizeComment(request.comment());
        ensureUniqueComment(
                review.getStudent().getId(), review.getCourse().getId(), comment, review.getId());
        review.update(request.rating(), comment);
        return toResponse(review);
    }

    @Transactional
    public void delete(Integer id) {
        courseReviewRepository.delete(getCourseReview(id));
    }

    private CourseReview getCourseReview(Integer id) {
        return courseReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CourseReview", id));
    }

    private void ensureUniqueComment(
            Integer studentId, Integer courseId, String comment, Integer excludedReviewId) {
        boolean duplicate = excludedReviewId == null
                ? courseReviewRepository.existsByStudent_IdAndCourse_IdAndCommentIgnoreCase(
                        studentId, courseId, comment)
                : courseReviewRepository.existsByStudent_IdAndCourse_IdAndCommentIgnoreCaseAndIdNot(
                        studentId, courseId, comment, excludedReviewId);
        if (duplicate) {
            throw new DuplicateResourceException(
                    "Course review with the same comment already exists for this student and course");
        }
    }

    private String normalizeComment(String comment) {
        return comment.strip();
    }

    private CourseReviewResponse toResponse(CourseReview review) {
        return new CourseReviewResponse(
                review.getId(),
                review.getCourse().getId(),
                review.getStudent().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}
