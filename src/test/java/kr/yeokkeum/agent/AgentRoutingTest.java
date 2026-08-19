package kr.yeokkeum.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AgentRoutingTest {

    @Test
    void extractsBalancedJsonFromProse() {
        String out = AgentService.extractJson("결정: {\"tool\":\"doc_search\",\"args\":{\"query\":\"a\"}} 입니다");
        assertThat(out).isEqualTo("{\"tool\":\"doc_search\",\"args\":{\"query\":\"a\"}}");
    }

    @Test
    void handlesBracesInsideStrings() {
        String out = AgentService.extractJson("{\"tool\":\"x\",\"args\":{\"q\":\"a{b}c\"}}");
        assertThat(out).isEqualTo("{\"tool\":\"x\",\"args\":{\"q\":\"a{b}c\"}}");
    }

    @Test
    void returnsNullWhenNoJson() {
        assertThat(AgentService.extractJson("도구 없음")).isNull();
    }
}
