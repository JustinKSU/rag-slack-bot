package com.keyholesoftware.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chroma-db")
public class ChromaProperties {
    private String host;
    private String port;
    private String databaseName;
    private String tenantName;

    private Map<String, ChromaCollectionProperties> collections;
}
