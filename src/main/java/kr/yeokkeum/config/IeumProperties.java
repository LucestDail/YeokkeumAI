package kr.yeokkeum.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 엮음AI 설정 바인딩 (prefix=ieum). */
@ConfigurationProperties(prefix = "ieum")
public class IeumProperties {

    private Llm llm = new Llm();
    private Auth auth = new Auth();
    private Rag rag = new Rag();

    public Llm getLlm() { return llm; }
    public void setLlm(Llm llm) { this.llm = llm; }
    public Auth getAuth() { return auth; }
    public void setAuth(Auth auth) { this.auth = auth; }
    public Rag getRag() { return rag; }
    public void setRag(Rag rag) { this.rag = rag; }

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

        public int getChunkChars() { return chunkChars; }
        public void setChunkChars(int v) { this.chunkChars = v; }
        public int getTopK() { return topK; }
        public void setTopK(int v) { this.topK = v; }
    }
}
