package com.example.learningplatform.service;

import com.example.learningplatform.dto.CourseStructureResponse;
import com.example.learningplatform.dto.LessonResponse;
import com.example.learningplatform.dto.ModuleStructureResponse;
import com.example.learningplatform.entity.Course;
import com.example.learningplatform.entity.Lesson;
import com.example.learningplatform.entity.Module;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.CourseRepository;
import com.example.learningplatform.repository.LessonRepository;
import com.example.learningplatform.repository.ModuleRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseStructureService {

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;

    public CourseStructureService(
            CourseRepository courseRepository,
            ModuleRepository moduleRepository,
            LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
    }

    public CourseStructureResponse findByCourseId(Integer courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        List<Module> modules = moduleRepository.findAllByCourse_IdOrderByOrderIndexAscIdAsc(courseId);
        List<Integer> moduleIds = modules.stream().map(Module::getId).toList();
        Map<Integer, List<LessonResponse>> lessonsByModuleId = moduleIds.isEmpty()
                ? Map.of()
                : lessonRepository.findAllByModule_IdInOrderByIdAsc(moduleIds).stream()
                        .collect(Collectors.groupingBy(
                                lesson -> lesson.getModule().getId(),
                                Collectors.mapping(this::toLessonResponse, Collectors.toList())));

        List<ModuleStructureResponse> moduleResponses = modules.stream()
                .map(module -> new ModuleStructureResponse(
                        module.getId(),
                        module.getTitle(),
                        module.getOrderIndex(),
                        module.getDescription(),
                        lessonsByModuleId.getOrDefault(module.getId(), List.of())))
                .toList();
        return new CourseStructureResponse(course.getId(), course.getTitle(), moduleResponses);
    }

    private LessonResponse toLessonResponse(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getModule().getId(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getVideoUrl());
    }
}
