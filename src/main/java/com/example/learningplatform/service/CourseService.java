package com.example.learningplatform.service;

import com.example.learningplatform.dto.CourseRequest;
import com.example.learningplatform.dto.CourseResponse;
import com.example.learningplatform.entity.Category;
import com.example.learningplatform.entity.Course;
import com.example.learningplatform.entity.Role;
import com.example.learningplatform.entity.Tag;
import com.example.learningplatform.entity.User;
import com.example.learningplatform.exception.DuplicateResourceException;
import com.example.learningplatform.exception.InvalidResourceStateException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.CategoryRepository;
import com.example.learningplatform.repository.CourseRepository;
import com.example.learningplatform.repository.TagRepository;
import com.example.learningplatform.repository.UserRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    public CourseService(
            CourseRepository courseRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            TagRepository tagRepository) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
    }

    public List<CourseResponse> findAll() {
        return courseRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CourseResponse findById(Integer id) {
        return toResponse(getCourse(id));
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        if (courseRepository.existsByTitleIgnoreCaseAndTeacher_IdAndStartDate(
                request.title(), request.teacherId(), request.startDate())) {
            throw new DuplicateResourceException(
                    "Course with the same title, teacher and start date already exists");
        }
        Category category = getCategory(request.categoryId());
        User teacher = getTeacher(request.teacherId());
        List<Tag> tags = getTags(request.tagIds());
        Course course = new Course(
                request.title(),
                request.description(),
                category,
                teacher,
                request.duration(),
                request.startDate());
        courseRepository.save(course);
        course.replaceTags(tags);
        return toResponse(course);
    }

    @Transactional
    public CourseResponse update(Integer id, CourseRequest request) {
        Course course = getCourse(id);
        if (courseRepository.existsByTitleIgnoreCaseAndTeacher_IdAndStartDateAndIdNot(
                request.title(), request.teacherId(), request.startDate(), id)) {
            throw new DuplicateResourceException(
                    "Course with the same title, teacher and start date already exists");
        }
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setCategory(getCategory(request.categoryId()));
        course.setTeacher(getTeacher(request.teacherId()));
        course.setDuration(request.duration());
        course.setStartDate(request.startDate());
        course.replaceTags(getTags(request.tagIds()));
        return toResponse(course);
    }

    @Transactional
    public void delete(Integer id) {
        courseRepository.delete(getCourse(id));
    }

    private Course getCourse(Integer id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
    }

    private Category getCategory(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private User getTeacher(Integer id) {
        User teacher = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (teacher.getRole() != Role.TEACHER) {
            throw new InvalidResourceStateException("Course teacher must have TEACHER role");
        }
        return teacher;
    }

    private List<Tag> getTags(Set<Integer> tagIds) {
        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            Integer missingId = tagIds.stream()
                    .filter(id -> tags.stream().noneMatch(tag -> tag.getId().equals(id)))
                    .findFirst()
                    .orElseThrow();
            throw new ResourceNotFoundException("Tag", missingId);
        }
        return tags;
    }

    private CourseResponse toResponse(Course course) {
        List<Integer> tagIds = course.getCourseTags().stream()
                .map(courseTag -> courseTag.getTag().getId())
                .sorted()
                .toList();
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCategory().getId(),
                course.getTeacher().getId(),
                course.getDuration(),
                course.getStartDate(),
                tagIds);
    }
}
