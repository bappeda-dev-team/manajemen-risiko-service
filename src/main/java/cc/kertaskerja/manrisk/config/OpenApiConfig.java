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
                          .description("Development server"),
                    new Server()
                          .url("https://manrisk-service.zeabur.app/manrisk/api")
                          .description("Production server")
              ))
              .addSecurityItem(new SecurityRequirement().addList("riskSession"))
              .components(new Components()
                    .addSecuritySchemes("riskSession",
                          new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Session-Id")
                                .description("Masukkan session ID hasil login Auth Service. Token internal hanya fallback sementara untuk BFF lama.")
                    )
              );
    }
}
