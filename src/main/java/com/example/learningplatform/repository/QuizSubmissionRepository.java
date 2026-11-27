package com.example.learningplatform.repository;

import com.example.learningplatform.entity.QuizSubmission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Integer> {

    List<QuizSubmission> findAllByQuiz_IdOrderByIdAsc(Integer quizId);

    boolean existsByQuiz_Id(Integer quizId);
}
