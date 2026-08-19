package kr.yeokkeum.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI(Swagger) 문서 — Bearer 토큰 스킴 포함 [EGOV-1]. UI: {context-path}/swagger-ui/index.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI yeokkeumOpenApi() {
        final String scheme = "bearer-token";
        return new OpenAPI()
                .info(new Info()
                        .title("엮음AI(YeokkeumAI) API")
                        .description("공공기관 업무보조 AI — 벤더무관 게이트웨이·하이브리드 RAG·규정검토·감사로그")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("ADMIN_TOKEN 또는 USER_TOKEN")));
    }
}
