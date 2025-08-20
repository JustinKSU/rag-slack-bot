package com.keyholesoftware.rag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.slack.api.Slack;

@Configuration
public class SlackConfig {

    @Bean
    public Slack slack() {
        return Slack.getInstance();
    }
}
