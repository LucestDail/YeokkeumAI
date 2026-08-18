package kr.yeokkeum.rag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 순수 자바 BM25 스코어러(온프렘·의존성 없음). */
public final class Bm25 {

    private static final double K1 = 1.5;
    private static final double B = 0.75;

    private Bm25() {}

    public static double[] scores(List<String> queryTokens, List<List<String>> docs) {
        int n = docs.size();
        double[] out = new double[n];
        if (n == 0) return out;

        int[] dl = new int[n];
        long total = 0;
        List<Map<String, Integer>> tfs = new java.util.ArrayList<>(n);
        Map<String, Integer> df = new HashMap<>();
        for (int i = 0; i < n; i++) {
            List<String> d = docs.get(i);
            dl[i] = d.size();
            total += dl[i];
            Map<String, Integer> tf = new HashMap<>();
            for (String term : d) tf.merge(term, 1, Integer::sum);
            tfs.add(tf);
            for (String term : tf.keySet()) df.merge(term, 1, Integer::sum);
        }
        double avgdl = n == 0 ? 1.0 : Math.max(1.0, (double) total / n);

        Set<String> qterms = new HashSet<>(queryTokens);
        for (String term : qterms) {
            Integer dfi = df.get(term);
            if (dfi == null) continue;
            double idf = Math.log(1 + (n - dfi + 0.5) / (dfi + 0.5));
            for (int i = 0; i < n; i++) {
                int tf = tfs.get(i).getOrDefault(term, 0);
                if (tf == 0) continue;
                double denom = tf + K1 * (1 - B + B * dl[i] / avgdl);
                out[i] += idf * (tf * (K1 + 1)) / denom;
            }
        }
        return out;
    }
}
