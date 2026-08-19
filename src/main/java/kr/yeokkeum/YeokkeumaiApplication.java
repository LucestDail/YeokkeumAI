package kr.yeokkeum;

import kr.yeokkeum.config.YeokkeumProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 엮음AI(YeokkeumAI) — 공공 업무보조 AI 플랫폼.
 * 기준 스택: Java 17 + Spring Boot 3.x (eGovFrame 4.3 호환 baseline).
 */
@SpringBootApplication
@EnableConfigurationProperties(YeokkeumProperties.class)
public class YeokkeumaiApplication {
    public static void main(String[] args) {
        SpringApplication.run(YeokkeumaiApplication.class, args);
    }
}
