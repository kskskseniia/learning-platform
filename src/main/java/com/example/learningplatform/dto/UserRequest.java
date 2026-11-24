package com.example.learningplatform.dto;

import com.example.learningplatform.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must contain at most 255 characters") String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must contain at most 255 characters") String email,
        @NotNull(message = "Role is required") Role role) {
}
