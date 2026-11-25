package com.example.learningplatform.repository;

import com.example.learningplatform.entity.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Integer> {

    boolean existsByStudent_Id(Integer studentId);

    boolean existsByStudent_IdAndCourse_IdAndCommentIgnoreCase(
            Integer studentId, Integer courseId, String comment);

    boolean existsByStudent_IdAndCourse_IdAndCommentIgnoreCaseAndIdNot(
            Integer studentId, Integer courseId, String comment, Integer id);
}
