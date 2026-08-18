package kr.yeokkeum.embedding;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** 벡터 유틸 — float[] ↔ byte[] 직렬화, 코사인 유사도. */
public final class Vectors {

    private Vectors() {}

    public static byte[] toBytes(float[] v) {
        ByteBuffer buf = ByteBuffer.allocate(v.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : v) buf.putFloat(f);
        return buf.array();
    }

    public static float[] fromBytes(byte[] b) {
        ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        float[] v = new float[b.length / 4];
        for (int i = 0; i < v.length; i++) v[i] = buf.getFloat();
        return v;
    }

    /** 코사인 유사도. 차원이 다르거나 0-벡터면 0. */
    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
