package com.example.learningplatform.service;

import com.example.learningplatform.dto.LessonRequest;
import com.example.learningplatform.dto.LessonResponse;
import com.example.learningplatform.entity.Lesson;
import com.example.learningplatform.entity.Module;
import com.example.learningplatform.exception.DuplicateResourceException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.LessonRepository;
import com.example.learningplatform.repository.ModuleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;

    public LessonService(LessonRepository lessonRepository, ModuleRepository moduleRepository) {
        this.lessonRepository = lessonRepository;
        this.moduleRepository = moduleRepository;
    }

    public List<LessonResponse> findByModuleId(Integer moduleId) {
        getModule(moduleId);
        return lessonRepository.findAllByModule_IdOrderByIdAsc(moduleId).stream()
                .map(this::toResponse)
                .toList();
    }

    public LessonResponse findById(Integer id) {
        return toResponse(getLesson(id));
    }

    @Transactional
    public LessonResponse create(Integer moduleId, LessonRequest request) {
        Module module = getModule(moduleId);
        verifyUniqueTitle(moduleId, request.title(), null);
        Lesson lesson = new Lesson(
                module,
                request.title(),
                request.content(),
                request.videoUrl());
        return toResponse(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonResponse update(Integer id, LessonRequest request) {
        Lesson lesson = getLesson(id);
        verifyUniqueTitle(lesson.getModule().getId(), request.title(), id);
        lesson.setTitle(request.title());
        lesson.setContent(request.content());
        lesson.setVideoUrl(request.videoUrl());
        return toResponse(lesson);
    }

    @Transactional
    public void delete(Integer id) {
        lessonRepository.delete(getLesson(id));
    }

    private Module getModule(Integer id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", id));
    }

    private Lesson getLesson(Integer id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", id));
    }

    private void verifyUniqueTitle(Integer moduleId, String title, Integer currentId) {
        boolean exists = currentId == null
                ? lessonRepository.existsByModule_IdAndTitle(moduleId, title)
                : lessonRepository.existsByModule_IdAndTitleAndIdNot(moduleId, title, currentId);
        if (exists) {
            throw new DuplicateResourceException(
                    "Lesson with this title already exists in the module");
        }
    }

    private LessonResponse toResponse(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getModule().getId(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getVideoUrl());
    }
}
