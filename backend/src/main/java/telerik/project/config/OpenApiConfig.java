package telerik.project.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import telerik.project.models.*;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI studyFlowOpenAPI() {
        // Security scheme for Firebase JWT
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("Firebase JWT Token (use format: Bearer <token>)");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("StudyFlow Forum API")
                        .description("REST API for StudyFlow Forum - A platform for developers to discuss ideas, solve problems, and share knowledge")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("StudyFlow Team")
                                .email("support@studyflow.dev"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.studyflow.dev")
                                .description("Production Server (if available)")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("studyflow-api")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            // Remove entity models from schemas to prevent circular reference issues
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().remove("User");
                openApi.getComponents().getSchemas().remove("Post");
                openApi.getComponents().getSchemas().remove("Comment");
                openApi.getComponents().getSchemas().remove("Tag");
                openApi.getComponents().getSchemas().remove("Notification");
            }
        };
    }
}

