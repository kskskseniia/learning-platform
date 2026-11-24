package com.example.learningplatform.service;

import com.example.learningplatform.dto.CourseTagResponse;
import com.example.learningplatform.repository.CourseTagRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseTagService {

    private final CourseTagRepository courseTagRepository;

    public CourseTagService(CourseTagRepository courseTagRepository) {
        this.courseTagRepository = courseTagRepository;
    }

    public List<CourseTagResponse> findAll() {
        return courseTagRepository.findAll().stream()
                .map(courseTag -> new CourseTagResponse(
                        courseTag.getId().getCourseId(),
                        courseTag.getId().getTagId()))
                .sorted(Comparator.comparing(CourseTagResponse::courseId)
                        .thenComparing(CourseTagResponse::tagId))
                .toList();
    }
}
