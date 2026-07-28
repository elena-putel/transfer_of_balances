package com.beltelecom.transfer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "informix.datasource")
public class InformixDataSourceProperties {

    private boolean enabled = true;
    private String url;
    private String username;
    private String password;
    private String driverClassName = "com.informix.jdbc.IfxDriver";
    private Hikari hikari = new Hikari();

    @Getter
    @Setter
    public static class Hikari {
        private int maximumPoolSize = 5;
        private int minimumIdle = 1;
        private String poolName = "informix-pool";
    }
}
