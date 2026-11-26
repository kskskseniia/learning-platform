package com.example.learningplatform.repository;

import com.example.learningplatform.entity.Module;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository extends JpaRepository<Module, Integer> {

    List<Module> findAllByCourse_IdOrderByOrderIndexAscIdAsc(Integer courseId);

    boolean existsByCourse_IdAndTitle(Integer courseId, String title);

    boolean existsByCourse_IdAndTitleAndIdNot(Integer courseId, String title, Integer id);
}
