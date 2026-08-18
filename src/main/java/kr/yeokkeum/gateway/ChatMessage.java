package kr.yeokkeum.gateway;

/** 정규화 채팅 메시지. role = system|user|assistant */
public record ChatMessage(String role, String content) {
    public static ChatMessage system(String c) { return new ChatMessage("system", c); }
    public static ChatMessage user(String c) { return new ChatMessage("user", c); }
}
