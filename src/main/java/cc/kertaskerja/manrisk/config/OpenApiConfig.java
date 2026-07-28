package cc.kertaskerja.manrisk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
              .info(new Info()
                    .title("Manajemen Risiko Apps Documentation")
                    .version("1.0.0")
                    .description("Pejabat Pengelola Informasi dan Dokumentasi Kabupaten Mahakam Ulu"))
              .servers(List.of(
                    new Server()
                          .url("http://localhost:8080/manrisk/api")
                          .description("Development server")
              ))
              .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
              .components(new Components()
                    .addSecuritySchemes("basicAuth",
                          new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("Enter username and password for Swagger UI access")
                    )
              );
    }
}
