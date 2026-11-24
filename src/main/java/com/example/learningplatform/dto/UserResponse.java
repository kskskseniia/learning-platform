package com.example.learningplatform.dto;

import com.example.learningplatform.entity.Role;

public record UserResponse(Integer id, String name, String email, Role role) {
}
