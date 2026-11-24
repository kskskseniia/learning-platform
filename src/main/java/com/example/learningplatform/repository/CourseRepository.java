package com.example.learningplatform.repository;

import com.example.learningplatform.entity.Course;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Integer> {

    boolean existsByTeacher_Id(Integer teacherId);

    boolean existsByTitleIgnoreCaseAndTeacher_IdAndStartDate(
            String title, Integer teacherId, LocalDate startDate);

    boolean existsByTitleIgnoreCaseAndTeacher_IdAndStartDateAndIdNot(
            String title, Integer teacherId, LocalDate startDate, Integer id);
}
