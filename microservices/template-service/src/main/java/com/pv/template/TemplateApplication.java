package com.pv.template;

import com.pv.common.config.EnableCommonLib;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableCommonLib
@EnableJpaRepositories(basePackages = "com.pv.template.repository.jpa")
@EnableMongoRepositories(basePackages = "com.pv.template.repository.mongo")
public class TemplateApplication {
    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }
}
