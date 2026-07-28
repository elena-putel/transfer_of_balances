package com.beltelecom.transfer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class AppConfig {

    @Bean
    public OpenAPI transferOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transfer of Balances API")
                        .description("""
                                API микросервиса загрузки переноса балансов из файлов .045.

                                Для ручного запуска обработки:
                                1. Откройте метод POST /api/v1/transfer/process
                                2. Нажмите Try it out → Execute
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Billing Team")));
    }
}
