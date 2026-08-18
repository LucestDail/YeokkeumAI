package kr.yeokkeum.gateway;

import java.util.Map;

public record ChatResult(String text, Map<String, Object> usage) {}
