package kr.yeokkeum.common;

import java.util.UUID;

public final class Ids {
    private Ids() {}

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
