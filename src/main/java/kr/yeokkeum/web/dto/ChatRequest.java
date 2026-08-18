package kr.yeokkeum.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message, String system, Boolean stream) {}
