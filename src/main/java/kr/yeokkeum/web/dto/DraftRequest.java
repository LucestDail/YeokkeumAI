package kr.yeokkeum.web.dto;

import jakarta.validation.constraints.NotBlank;

public record DraftRequest(String kind, @NotBlank String brief) {}
