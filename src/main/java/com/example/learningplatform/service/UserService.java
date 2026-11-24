package com.example.learningplatform.service;

import com.example.learningplatform.dto.UserRequest;
import com.example.learningplatform.dto.UserResponse;
import com.example.learningplatform.entity.Role;
import com.example.learningplatform.entity.User;
import com.example.learningplatform.exception.DuplicateResourceException;
import com.example.learningplatform.exception.InvalidResourceStateException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.CourseRepository;
import com.example.learningplatform.repository.ProfileRepository;
import com.example.learningplatform.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final CourseRepository courseRepository;

    public UserService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            CourseRepository courseRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.courseRepository = courseRepository;
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse findById(Integer id) {
        return toResponse(getUser(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        ensureEmailAvailable(request.email(), null);
        User user = new User(request.name(), request.email(), request.role());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Integer id, UserRequest request) {
        User user = getUser(id);
        ensureEmailAvailable(request.email(), id);
        if (user.getRole() == Role.TEACHER
                && request.role() != Role.TEACHER
                && courseRepository.existsByTeacher_Id(id)) {
            throw new InvalidResourceStateException(
                    "Cannot change role of a teacher assigned to courses");
        }
        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(request.role());
        return toResponse(user);
    }

    @Transactional
    public void delete(Integer id) {
        User user = getUser(id);
        profileRepository.findByUserId(id).ifPresent(profileRepository::delete);
        userRepository.delete(user);
    }

    private User getUser(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void ensureEmailAvailable(String email, Integer currentId) {
        userRepository.findByEmail(email)
                .filter(user -> !user.getId().equals(currentId))
                .ifPresent(user -> {
                    throw new DuplicateResourceException("User with this email already exists");
                });
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
