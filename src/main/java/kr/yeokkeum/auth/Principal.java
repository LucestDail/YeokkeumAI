package kr.yeokkeum.auth;

public record Principal(String actor, String role) {
    public static final String ATTR = "yeokkeum.principal";
}
