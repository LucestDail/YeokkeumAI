package kr.yeokkeum.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 라틴 단어 + CJK 문자(단일 + bigram). 외부 형태소분석기 없이 온프렘 동작. */
public final class Tokenizer {

    private static final Pattern WORD = Pattern.compile("[a-z0-9]+");
    private static final Pattern CJK = Pattern.compile("[\\uAC00-\\uD7A3\\u3040-\\u30FF\\u4E00-\\u9FFF]");

    private Tokenizer() {}

    public static List<String> tokenize(String text) {
        String t = text == null ? "" : text.toLowerCase();
        List<String> toks = new ArrayList<>();
        Matcher wm = WORD.matcher(t);
        while (wm.find()) toks.add(wm.group());
        List<String> cjk = new ArrayList<>();
        Matcher cm = CJK.matcher(t);
        while (cm.find()) cjk.add(cm.group());
        toks.addAll(cjk);
        for (int i = 0; i < cjk.size() - 1; i++) toks.add(cjk.get(i) + cjk.get(i + 1));
        return toks;
    }
}
