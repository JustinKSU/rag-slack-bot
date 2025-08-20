package com.keyholesoftware.rag.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChromaCollectionProperties {
    private String promptRefinements;
    private boolean initializeSchema = false;
}