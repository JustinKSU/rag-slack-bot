package com.keyholesoftware.rag.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Rag Slack Bot API", version = "v1", description = "API documentation for the Rag Slack Bot application"))
public class OpenAPIConfig {

}
