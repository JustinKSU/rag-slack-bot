package com.keyholesoftware.rag.config;

import static com.keyholesoftware.rag.config.ConfigConstants.*;

import java.util.Map;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.micrometer.observation.ObservationRegistry;

@Configuration
public class ChromaConfig {

        private final ChromaProperties chromaProperties;

        public ChromaConfig(ChromaProperties chromaProperties) {
                this.chromaProperties = chromaProperties;
        }

        @Bean
        @Primary
        public ChromaApi chromaApi() {
                return ChromaApi.builder()
                                .baseUrl(String.format("%s:%s", chromaProperties.getHost(), chromaProperties.getPort()))
                                .build();
        }

        @Bean
        @Qualifier(OPENAI)
        public ChromaVectorStore openaiVectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel,
                        ObjectProvider<ObservationRegistry> observationRegistry,
                        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                        BatchingStrategy chromaBatchingStrategy) {
                return ChromaVectorStore.builder(chromaApi, embeddingModel)
                                .tenantName(chromaProperties.getTenantName())
                                .databaseName(chromaProperties.getDatabaseName())
                                .collectionName(OPENAI)
                                .initializeSchema(chromaProperties.getCollections().get(OPENAI).isInitializeSchema())
                                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                                .batchingStrategy(chromaBatchingStrategy)
                                .build();
        }

        @Bean
        @Qualifier(VECTOR_STORE_MAP)
        public Map<String, VectorStore> vectorStoreMap(
                        @Qualifier(OPENAI) ChromaVectorStore openaiStore
                        ) {
                return Map.of(
                        OPENAI, openaiStore);
        }

        @Bean
        @Qualifier(OPENAI)
        public QuestionAnswerAdvisor openaiAdvisor(@Qualifier(OPENAI) ChromaVectorStore vectorStore) {
                return QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                                .similarityThreshold(0d)
                                                .topK(6)
                                                .build())
                                .build();
        }

        @Bean
        @Qualifier(ADVISORS_MAP)
        public Map<String, QuestionAnswerAdvisor> questionAnswerAdvisorMap(
                        @Qualifier(OPENAI) QuestionAnswerAdvisor openaiAdvisor) {
                return Map.of(
                        OPENAI, openaiAdvisor);
        }
}
