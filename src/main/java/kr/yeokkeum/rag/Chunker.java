package kr.yeokkeum.rag;

import java.util.ArrayList;
import java.util.List;

/** 문단 경계 우선 청킹. */
public final class Chunker {

    private Chunker() {}

    public static List<String> chunk(String text, int chunkChars) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;
        String[] paras = text.split("\\n\\s*\\n");
        StringBuilder buf = new StringBuilder();
        for (String pRaw : paras) {
            String p = pRaw.strip();
            if (p.isEmpty()) continue;
            if (buf.length() > 0 && buf.length() + p.length() + 1 > chunkChars) {
                chunks.add(buf.toString());
                buf.setLength(0);
            }
            if (buf.length() > 0) buf.append("\n");
            buf.append(p);
            while (buf.length() > chunkChars * 1.5) {
                chunks.add(buf.substring(0, chunkChars));
                buf.delete(0, chunkChars);
            }
        }
        if (buf.length() > 0) chunks.add(buf.toString());
        return chunks;
    }
}
