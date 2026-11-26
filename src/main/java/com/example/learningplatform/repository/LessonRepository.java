package com.example.learningplatform.repository;

import com.example.learningplatform.entity.Lesson;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {

    List<Lesson> findAllByModule_IdOrderByIdAsc(Integer moduleId);

    List<Lesson> findAllByModule_IdInOrderByIdAsc(Collection<Integer> moduleIds);

    boolean existsByModule_IdAndTitle(Integer moduleId, String title);

    boolean existsByModule_IdAndTitleAndIdNot(Integer moduleId, String title, Integer id);
}
