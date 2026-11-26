package com.example.learningplatform.repository;

import com.example.learningplatform.entity.Submission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {

    boolean existsByAssignment_IdAndStudent_Id(Integer assignmentId, Integer studentId);

    boolean existsByAssignment_IdAndScoreGreaterThan(Integer assignmentId, Integer score);

    List<Submission> findAllByAssignment_IdOrderByIdAsc(Integer assignmentId);
}
