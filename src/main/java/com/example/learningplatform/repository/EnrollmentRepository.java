package com.example.learningplatform.repository;

import com.example.learningplatform.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

    boolean existsByStudent_IdAndCourse_Id(Integer studentId, Integer courseId);

    boolean existsByStudent_Id(Integer studentId);
}
