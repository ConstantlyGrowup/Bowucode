package com.museum.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChromaConfig {

    private static final Logger log = LoggerFactory.getLogger(ChromaConfig.class);

    @Value("${langchain4j.chroma.host:http://localhost}")
    private String chromaHost;

    @Value("${langchain4j.chroma.port:8000}")
    private int chromaPort;

    @Value("${langchain4j.chroma.collection-name:museum-collection}")
    private String collectionName;

    @Value("${langchain4j.chroma.enabled:false}")
    private boolean chromaEnabled;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        if (!chromaEnabled) {
            log.info("Chroma is disabled, using in-memory embedding store");
            return new InMemoryEmbeddingStore<>();
        }

        try {
            // 确保 URL 格式正确
            String host = chromaHost;
            if (!host.startsWith("http://") && !host.startsWith("https://")) {
                host = "http://" + host;
            }
            String baseUrl = host + ":" + chromaPort;
            if (!baseUrl.endsWith("/")) {
                baseUrl = baseUrl + "/";
            }
            
            log.info("Attempting to connect to Chroma at: {}", baseUrl);
            // Constructor: (baseUrl, collectionName, timeout, logRequests, logResponses)
            ChromaEmbeddingStore chromaStore = new ChromaEmbeddingStore(baseUrl, collectionName, Duration.ofSeconds(10), false, false);
            log.info("Successfully connected to Chroma");
            return chromaStore;
        } catch (Exception e) {
            log.warn("Failed to connect to Chroma: {}. Falling back to in-memory store.", e.getMessage());
            return new InMemoryEmbeddingStore<>();
        }
    }
}


