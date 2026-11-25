package com.example.learningplatform.service;

import com.example.learningplatform.dto.EnrollmentRequest;
import com.example.learningplatform.dto.EnrollmentResponse;
import com.example.learningplatform.dto.EnrollmentStatusRequest;
import com.example.learningplatform.entity.Course;
import com.example.learningplatform.entity.Enrollment;
import com.example.learningplatform.entity.EnrollmentStatus;
import com.example.learningplatform.entity.Role;
import com.example.learningplatform.entity.User;
import com.example.learningplatform.exception.DuplicateResourceException;
import com.example.learningplatform.exception.InvalidResourceStateException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.CourseRepository;
import com.example.learningplatform.repository.EnrollmentRepository;
import com.example.learningplatform.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public EnrollmentResponse create(EnrollmentRequest request) {
        if (enrollmentRepository.existsByStudent_IdAndCourse_Id(
                request.studentId(), request.courseId())) {
            throw new DuplicateResourceException("Student is already enrolled in this course");
        }
        User student = userRepository.findByIdForUpdate(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.studentId()));
        if (student.getRole() != Role.STUDENT) {
            throw new InvalidResourceStateException("Enrollment user must have STUDENT role");
        }
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.courseId()));
        Enrollment enrollment = new Enrollment(
                student,
                course,
                LocalDate.now(),
                EnrollmentStatus.ACTIVE);
        return toResponse(enrollmentRepository.save(enrollment));
    }

    public List<EnrollmentResponse> findAll() {
        return enrollmentRepository.findAll(Sort.by("id")).stream()
                .map(this::toResponse)
                .toList();
    }

    public EnrollmentResponse findById(Integer id) {
        return toResponse(getEnrollment(id));
    }

    @Transactional
    public EnrollmentResponse updateStatus(Integer id, EnrollmentStatusRequest request) {
        Enrollment enrollment = getEnrollment(id);
        enrollment.setStatus(request.status());
        return toResponse(enrollment);
    }

    @Transactional
    public void delete(Integer id) {
        enrollmentRepository.delete(getEnrollment(id));
    }

    private Enrollment getEnrollment(Integer id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getCourse().getId(),
                enrollment.getEnrollDate(),
                enrollment.getStatus());
    }
}
