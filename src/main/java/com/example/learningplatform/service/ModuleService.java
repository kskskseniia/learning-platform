package com.example.learningplatform.service;

import com.example.learningplatform.dto.ModuleRequest;
import com.example.learningplatform.dto.ModuleResponse;
import com.example.learningplatform.entity.Course;
import com.example.learningplatform.entity.Module;
import com.example.learningplatform.exception.DuplicateResourceException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.CourseRepository;
import com.example.learningplatform.repository.ModuleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;

    public ModuleService(ModuleRepository moduleRepository, CourseRepository courseRepository) {
        this.moduleRepository = moduleRepository;
        this.courseRepository = courseRepository;
    }

    public List<ModuleResponse> findByCourseId(Integer courseId) {
        getCourse(courseId);
        return moduleRepository.findAllByCourse_IdOrderByOrderIndexAscIdAsc(courseId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ModuleResponse findById(Integer id) {
        return toResponse(getModule(id));
    }

    @Transactional
    public ModuleResponse create(Integer courseId, ModuleRequest request) {
        Course course = getCourse(courseId);
        verifyUniqueTitle(courseId, request.title(), null);
        Module module = new Module(
                course,
                request.title(),
                request.orderIndex(),
                request.description());
        return toResponse(moduleRepository.save(module));
    }

    @Transactional
    public ModuleResponse update(Integer id, ModuleRequest request) {
        Module module = getModule(id);
        verifyUniqueTitle(module.getCourse().getId(), request.title(), id);
        module.setTitle(request.title());
        module.setOrderIndex(request.orderIndex());
        module.setDescription(request.description());
        return toResponse(module);
    }

    @Transactional
    public void delete(Integer id) {
        moduleRepository.delete(getModule(id));
    }

    private Course getCourse(Integer id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
    }

    private Module getModule(Integer id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", id));
    }

    private void verifyUniqueTitle(Integer courseId, String title, Integer currentId) {
        boolean exists = currentId == null
                ? moduleRepository.existsByCourse_IdAndTitle(courseId, title)
                : moduleRepository.existsByCourse_IdAndTitleAndIdNot(courseId, title, currentId);
        if (exists) {
            throw new DuplicateResourceException(
                    "Module with this title already exists in the course");
        }
    }

    private ModuleResponse toResponse(Module module) {
        return new ModuleResponse(
                module.getId(),
                module.getCourse().getId(),
                module.getTitle(),
                module.getOrderIndex(),
                module.getDescription());
    }
}
