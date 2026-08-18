package kr.yeokkeum.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SummarizeRequest(@NotBlank String text) {}
