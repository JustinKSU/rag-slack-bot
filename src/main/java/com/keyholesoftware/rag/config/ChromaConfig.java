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
        @Qualifier(MINNKOTA)
        public ChromaVectorStore minnkotaVectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel,
                        ObjectProvider<ObservationRegistry> observationRegistry,
                        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                        BatchingStrategy chromaBatchingStrategy) {
                return ChromaVectorStore.builder(chromaApi, embeddingModel)
                                .tenantName(chromaProperties.getTenantName())
                                .databaseName(chromaProperties.getDatabaseName())
                                .collectionName(MINNKOTA)
                                .initializeSchema(chromaProperties.getCollections().get(MINNKOTA).isInitializeSchema())
                                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                                .batchingStrategy(chromaBatchingStrategy)
                                .build();
        }

        @Bean
        @Qualifier(EMPLOYEE_HANDBOOK)
        public ChromaVectorStore employeehandbookVectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel,
                        ObjectProvider<ObservationRegistry> observationRegistry,
                        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                        BatchingStrategy chromaBatchingStrategy) {
                return ChromaVectorStore.builder(chromaApi, embeddingModel)
                                .tenantName(chromaProperties.getTenantName())
                                .databaseName(chromaProperties.getDatabaseName())
                                .collectionName(EMPLOYEE_HANDBOOK)
                                .initializeSchema(chromaProperties.getCollections().get(EMPLOYEE_HANDBOOK)
                                                .isInitializeSchema())
                                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                                .batchingStrategy(chromaBatchingStrategy)
                                .build();
        }

        @Bean
        @Qualifier(VECTOR_STORE_MAP)
        public Map<String, VectorStore> vectorStoreMap(
                        @Qualifier(MINNKOTA) ChromaVectorStore minnkotaStore,
                        @Qualifier(EMPLOYEE_HANDBOOK) ChromaVectorStore employeeHandbookStore) {
                return Map.of(
                                MINNKOTA, minnkotaStore,
                                EMPLOYEE_HANDBOOK, employeeHandbookStore);
        }

        @Bean
        @Qualifier(MINNKOTA)
        public QuestionAnswerAdvisor minnkotaAdvisor(@Qualifier(MINNKOTA) ChromaVectorStore vectorStore) {
                return QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                                .similarityThreshold(0d)
                                                .topK(6)
                                                .build())
                                .build();
        }

        @Bean
        @Qualifier(EMPLOYEE_HANDBOOK)
        public QuestionAnswerAdvisor employeehandbookAdvisor(
                        @Qualifier(EMPLOYEE_HANDBOOK) ChromaVectorStore vectorStore) {
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
                        @Qualifier(MINNKOTA) QuestionAnswerAdvisor minnkotaAdvisor,
                        @Qualifier(EMPLOYEE_HANDBOOK) QuestionAnswerAdvisor employeeHandbookAdvisor) {
                return Map.of(
                                MINNKOTA, minnkotaAdvisor,
                                EMPLOYEE_HANDBOOK, employeeHandbookAdvisor);
        }
}
