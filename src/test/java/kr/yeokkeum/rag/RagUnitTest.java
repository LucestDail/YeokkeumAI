package kr.yeokkeum.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RagUnitTest {

    @Test
    void tokenizeMixesLatinAndCjk() {
        List<String> toks = Tokenizer.tokenize("웹접근성 KWCAG 2.2");
        assertThat(toks).contains("kwcag", "2", "웹", "접");
        assertThat(toks).contains("웹접");  // CJK bigram
    }

    @Test
    void chunkSplitsByParagraph() {
        String text = "문단1 내용.\n\n문단2 내용.\n\n문단3 내용.";
        List<String> chunks = Chunker.chunk(text, 12);
        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void bm25RanksRelevantDocHighest() {
        List<List<String>> docs = List.of(
                Tokenizer.tokenize("웹접근성은 KWCAG 2.2 기준을 따른다"),
                Tokenizer.tokenize("개인정보는 암호화하여 보관한다"),
                Tokenizer.tokenize("나라장터는 조달 시스템이다"));
        double[] s = Bm25.scores(Tokenizer.tokenize("웹접근성 기준"), docs);
        int best = 0;
        for (int i = 1; i < s.length; i++) if (s[i] > s[best]) best = i;
        assertThat(best).isEqualTo(0);
        assertThat(s[0]).isGreaterThan(0.0);
    }
}
