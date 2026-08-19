package kr.yeokkeum.agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 등록된 도구 레지스트리(내장 + 향후 MCP 래퍼). */
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<Tool> discovered) {
        for (Tool t : discovered) tools.put(t.name(), t);
    }

    public Tool get(String name) {
        return tools.get(name);
    }

    public List<Tool> all() {
        return List.copyOf(tools.values());
    }
}
