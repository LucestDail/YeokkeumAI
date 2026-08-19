package kr.yeokkeum.agent;

import java.util.Map;

/**
 * 업무 에이전트 도구. risky=true(변경/실행/전송)는 HITL 승인 후에만 실행된다(통제·책임성).
 * 외부 MCP 도구도 이 인터페이스로 래핑해 등록하는 확장점.
 */
public interface Tool {

    String name();

    String description();

    /** true 면 실행 전 사람 승인 필요(HITL). */
    boolean risky();

    /** 도구 실행. 결과 텍스트 반환. */
    String execute(Map<String, Object> args);

    static String str(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        return v == null ? "" : String.valueOf(v);
    }
}
