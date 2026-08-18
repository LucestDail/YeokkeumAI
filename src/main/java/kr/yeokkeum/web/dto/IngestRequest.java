package kr.yeokkeum.web.dto;

import jakarta.validation.constraints.NotBlank;

public record IngestRequest(@NotBlank String filename, @NotBlank String text) {}
