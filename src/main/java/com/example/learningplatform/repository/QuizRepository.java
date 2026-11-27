package com.example.learningplatform.repository;

import com.example.learningplatform.entity.Quiz;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, Integer> {

    @EntityGraph(attributePaths = {"questions", "questions.answerOptions"})
    Optional<Quiz> findByModule_Id(Integer moduleId);

    boolean existsByModule_Id(Integer moduleId);

    @EntityGraph(attributePaths = {"questions", "questions.answerOptions"})
    @Query("select quiz from Quiz quiz where quiz.id = :id")
    Optional<Quiz> findWithQuestionsById(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select quiz from Quiz quiz where quiz.id = :id")
    Optional<Quiz> findByIdForUpdate(@Param("id") Integer id);
}
