package com.recallhub;

import com.recallhub.config.RecallHubProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(RecallHubProperties.class)
@SpringBootApplication
public class RecallHubApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(RecallHubApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(RecallHubApplication.class, args);
    }
}
