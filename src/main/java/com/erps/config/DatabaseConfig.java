package com.erps.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Production DataSource configuration with automatic support for Cloud PostgreSQL URIs
 * (e.g., Render, Neon, Heroku standard postgresql://user:pass@host:port/dbname formats).
 */
@Configuration
@Profile("prod")
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:}")
    private String configuredUrl;

    @Value("${spring.datasource.username:}")
    private String configuredUsername;

    @Value("${spring.datasource.password:}")
    private String configuredPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getenv("DB_URL");
        }
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = configuredUrl;
        }

        // Strip surrounding quotes if present in env vars
        if (dbUrl != null) {
            dbUrl = dbUrl.trim();
            if (dbUrl.startsWith("\"") && dbUrl.endsWith("\"")) {
                dbUrl = dbUrl.substring(1, dbUrl.length() - 1);
            }
        }

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");

        if (dbUrl != null && (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://"))) {
            try {
                URI uri = new URI(dbUrl);
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String query = uri.getQuery() != null ? "?" + uri.getQuery() : "?sslmode=require";

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + query;
                config.setJdbcUrl(jdbcUrl);

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    config.setUsername(userInfo[0]);
                    if (userInfo.length > 1) {
                        config.setPassword(userInfo[1]);
                    }
                } else {
                    config.setUsername(configuredUsername);
                    config.setPassword(configuredPassword);
                }
                log.info("Configured PostgreSQL JDBC connection from URI for host: {}", host);
            } catch (Exception e) {
                log.warn("Could not parse dbUrl as URI, falling back to prefixing jdbc:: {}", e.getMessage());
                config.setJdbcUrl("jdbc:" + dbUrl);
                config.setUsername(configuredUsername);
                config.setPassword(configuredPassword);
            }
        } else if (dbUrl != null) {
            config.setJdbcUrl(dbUrl.startsWith("jdbc:") ? dbUrl : "jdbc:" + dbUrl);
            config.setUsername(configuredUsername);
            config.setPassword(configuredPassword);
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);

        return new HikariDataSource(config);
    }
}
