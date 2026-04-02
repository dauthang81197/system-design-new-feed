package com.newsfeed.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication
public class AuthServiceApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(AuthServiceApplication.class, args);
        logDatabaseConfig(ctx.getEnvironment());
    }

    private static void logDatabaseConfig(Environment env) {
        String host   = env.getProperty("POSTGRES_HOST",  "localhost");
        String port   = env.getProperty("POSTGRES_PORT",  "5432");
        String db     = env.getProperty("POSTGRES_DB",    "auth_db");
        String user   = env.getProperty("POSTGRES_USER",  "postgres");
        String url    = env.getProperty("spring.datasource.url", "N/A");
        String ddl    = env.getProperty("spring.jpa.hibernate.ddl-auto", "update");

        log.info("==========================================================");
        log.info("  DATABASE CONFIG");
        log.info("  POSTGRES_HOST : {}", host);
        log.info("  POSTGRES_PORT : {}", port);
        log.info("  POSTGRES_DB   : {}", db);
        log.info("  POSTGRES_USER : {}", user);
        log.info("  JDBC URL      : {}", url);
        log.info("  DDL AUTO      : {}", ddl);
        log.info("  PASSWORD      : [REDACTED]");
        log.info("==========================================================");
    }
}

