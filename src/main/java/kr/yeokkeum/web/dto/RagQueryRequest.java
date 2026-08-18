package kr.yeokkeum.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RagQueryRequest(@NotBlank String query, Integer topK) {}
