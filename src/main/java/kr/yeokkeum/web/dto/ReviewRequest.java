package kr.yeokkeum.web.dto;

import jakarta.validation.constraints.NotBlank;

/** 규정검토 요청. text=검토할 작성물, lang=답변 언어(선택, 기본 한국어). */
public record ReviewRequest(@NotBlank String text, String lang) {}
