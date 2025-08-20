package com.keyholesoftware.rag.config;

import org.springframework.stereotype.Component;

@Component
public class PromptRefinementsProvider {

    private final ChromaProperties chromaProperties;

    public PromptRefinementsProvider(ChromaProperties chromaProperties) {
        this.chromaProperties = chromaProperties;
    }

    public String getPromptRefinement(String storeName) {
        var storeProperties = chromaProperties.getCollections().get(storeName);
        return storeProperties != null ? storeProperties.getPromptRefinements() : null;
    }
}
