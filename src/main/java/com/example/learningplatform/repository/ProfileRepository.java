package com.example.learningplatform.repository;

import com.example.learningplatform.entity.Profile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Integer> {

    Optional<Profile> findByUserId(Integer userId);
}
