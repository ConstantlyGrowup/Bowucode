package com.museum.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${langchain4j.dashscope.model.chat:qwen-turbo}")
    private String chatModelName;

    @Value("${langchain4j.dashscope.model.embedding:text-embedding-v1}")
    private String embeddingModelName;

    @Bean
    public QwenChatModel qwenChatModel() {
        return QwenChatModel.builder()
                .apiKey(dashScopeApiKey)
                .modelName(chatModelName)
                .build();
    }

    @Bean
    public QwenEmbeddingModel qwenEmbeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(dashScopeApiKey)
                .modelName(embeddingModelName)
                .build();
    }
}


