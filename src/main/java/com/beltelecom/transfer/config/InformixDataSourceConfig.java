package com.beltelecom.transfer.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InformixDataSourceConfig {

    @Bean(name = "informixDataSource")
    public DataSource informixDataSource(InformixDataSourceProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getDriverClassName());
        config.setMaximumPoolSize(properties.getHikari().getMaximumPoolSize());
        config.setMinimumIdle(properties.getHikari().getMinimumIdle());
        config.setPoolName(properties.getHikari().getPoolName());
        return new HikariDataSource(config);
    }

    @Bean(name = "informixJdbcTemplate")
    public NamedParameterJdbcTemplate informixJdbcTemplate(
            @Qualifier("informixDataSource") DataSource informixDataSource) {
        return new NamedParameterJdbcTemplate(informixDataSource);
    }

    @Bean(name = "informixTransactionManager")
    public PlatformTransactionManager informixTransactionManager(
            @Qualifier("informixDataSource") DataSource informixDataSource) {
        return new DataSourceTransactionManager(informixDataSource);
    }
}
