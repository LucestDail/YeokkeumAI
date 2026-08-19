package kr.yeokkeum.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 엮음AI 설정 바인딩 (prefix=yeokkeum). */
@ConfigurationProperties(prefix = "yeokkeum")
public class YeokkeumProperties {

    private Llm llm = new Llm();
    private Auth auth = new Auth();
    private Rag rag = new Rag();
    private Doc doc = new Doc();
    private Embedding embedding = new Embedding();

    public Llm getLlm() { return llm; }
    public void setLlm(Llm llm) { this.llm = llm; }
    public Auth getAuth() { return auth; }
    public void setAuth(Auth auth) { this.auth = auth; }
    public Rag getRag() { return rag; }
    public void setRag(Rag rag) { this.rag = rag; }
    public Doc getDoc() { return doc; }
    public void setDoc(Doc doc) { this.doc = doc; }
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }

    public static class Llm {
        private String provider = "auto";
        private String baseUrl = "https://openrouter.ai/api/v1";
        private String apiKey = "";
        private String model = "deepseek/deepseek-chat";
        private int timeoutSeconds = 120;

        public String getProvider() { return provider; }
        public void setProvider(String v) { this.provider = v; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public String getModel() { return model; }
        public void setModel(String v) { this.model = v; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int v) { this.timeoutSeconds = v; }
    }

    public static class Auth {
        private String adminToken = "";
        private String userToken = "";
        private boolean insecureOpenMode = false;

        public String getAdminToken() { return adminToken; }
        public void setAdminToken(String v) { this.adminToken = v; }
        public String getUserToken() { return userToken; }
        public void setUserToken(String v) { this.userToken = v; }
        public boolean isInsecureOpenMode() { return insecureOpenMode; }
        public void setInsecureOpenMode(boolean v) { this.insecureOpenMode = v; }
    }

    public static class Rag {
        private int chunkChars = 1200;
        private int topK = 5;
        /** BM25 + dense(BGE-M3) 하이브리드 융합 사용. 임베딩·벡터가 없으면 자동으로 BM25 단독으로 폴백. */
        private boolean hybrid = true;
        /** RRF(Reciprocal Rank Fusion) 상수 k. */
        private int rrfK = 60;

        public int getChunkChars() { return chunkChars; }
        public void setChunkChars(int v) { this.chunkChars = v; }
        public int getTopK() { return topK; }
        public void setTopK(int v) { this.topK = v; }
        public boolean isHybrid() { return hybrid; }
        public void setHybrid(boolean v) { this.hybrid = v; }
        public int getRrfK() { return rrfK; }
        public void setRrfK(int v) { this.rrfK = v; }
    }

    /** 임베딩(dense 검색) — 벤더무관 OpenAI 호환 /embeddings(BGE-M3 등, TEI·vLLM·게이트웨이). 미구성 시 오프라인 stub. */
    public static class Embedding {
        private String provider = "auto";                 // auto | openai_compat | stub
        private String baseUrl = "";                       // 예: http://<tei-host>/v1 (또는 /embeddings 상위)
        private String apiKey = "";
        private String model = "bge-m3";
        private int timeoutSeconds = 60;
        private int stubDimension = 256;                   // stub 결정적 벡터 차원

        public String getProvider() { return provider; }
        public void setProvider(String v) { this.provider = v; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public String getModel() { return model; }
        public void setModel(String v) { this.model = v; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int v) { this.timeoutSeconds = v; }
        public int getStubDimension() { return stubDimension; }
        public void setStubDimension(int v) { this.stubDimension = v; }
    }

    /** 문서 처리 — HWP/HWPX 파싱은 rhwp 바이너리(export-text) 서브프로세스로 위임. */
    public static class Doc {
        /** rhwp 실행 경로. 기본 bin/rhwp(작업디렉토리 기준), 없으면 PATH의 rhwp. 환경변수 RHWP_PATH 로 오버라이드. */
        private String rhwpPath = "bin/rhwp";
        /** rhwp 실행 타임아웃(ms). */
        private long rhwpTimeoutMs = 30_000;

        public String getRhwpPath() { return rhwpPath; }
        public void setRhwpPath(String v) { this.rhwpPath = v; }
        public long getRhwpTimeoutMs() { return rhwpTimeoutMs; }
        public void setRhwpTimeoutMs(long v) { this.rhwpTimeoutMs = v; }
    }
}
