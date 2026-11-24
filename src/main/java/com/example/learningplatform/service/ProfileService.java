package com.example.learningplatform.service;

import com.example.learningplatform.dto.ProfileRequest;
import com.example.learningplatform.dto.ProfileResponse;
import com.example.learningplatform.dto.ProfileUpdateRequest;
import com.example.learningplatform.entity.Profile;
import com.example.learningplatform.entity.User;
import com.example.learningplatform.exception.DuplicateResourceException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.ProfileRepository;
import com.example.learningplatform.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    public List<ProfileResponse> findAll() {
        return profileRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ProfileResponse findById(Integer id) {
        return toResponse(getProfile(id));
    }

    @Transactional
    public ProfileResponse create(ProfileRequest request) {
        ensureUserHasNoProfile(request.userId());
        Profile profile = new Profile(getUser(request.userId()), request.bio(), request.avatarUrl());
        return toResponse(profileRepository.save(profile));
    }

    @Transactional
    public ProfileResponse update(Integer id, ProfileUpdateRequest request) {
        Profile profile = getProfile(id);
        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());
        return toResponse(profile);
    }

    @Transactional
    public void delete(Integer id) {
        profileRepository.delete(getProfile(id));
    }

    private Profile getProfile(Integer id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", id));
    }

    private User getUser(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void ensureUserHasNoProfile(Integer userId) {
        profileRepository.findByUserId(userId)
                .ifPresent(profile -> {
                    throw new DuplicateResourceException("User already has a profile");
                });
    }

    private ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getBio(),
                profile.getAvatarUrl());
    }
}
