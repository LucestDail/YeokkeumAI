package kr.yeokkeum.rag;

import java.util.List;

public record RagResult(String answer, List<Citation> citations, boolean grounded, String model) {}
