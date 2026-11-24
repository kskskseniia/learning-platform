package com.example.learningplatform.exception;

import java.util.Map;

public record ValidationError(String message, Map<String, String> errors) {
}
