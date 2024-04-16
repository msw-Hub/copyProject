package io.cloudtype.Demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SWR 프로젝트 API")
                        .description("반려동물의 산책 및 돌봄을 위한 서비스입니다.")
                        .version("1.0.0"));
    }
}