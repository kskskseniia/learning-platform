package com.example.learningplatform.repository;

import com.example.learningplatform.entity.Assignment;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {

    List<Assignment> findAllByLesson_IdOrderByIdAsc(Integer lessonId);

    boolean existsByLesson_IdAndTitle(Integer lessonId, String title);

    boolean existsByLesson_IdAndTitleAndIdNot(Integer lessonId, String title, Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT assignment FROM Assignment assignment WHERE assignment.id = :id")
    Optional<Assignment> findByIdForUpdate(@Param("id") Integer id);
}
