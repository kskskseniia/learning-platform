package com.example.learningplatform.repository;

import com.example.learningplatform.entity.CourseTag;
import com.example.learningplatform.entity.CourseTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseTagRepository extends JpaRepository<CourseTag, CourseTagId> {
}
