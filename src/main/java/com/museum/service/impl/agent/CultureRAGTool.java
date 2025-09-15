package com.museum.service.impl.agent;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CultureRAGTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(CultureRAGTool.class);

    private final QwenChatModel complexModel;
    private final QwenEmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public CultureRAGTool(@Qualifier("complexQwenChatModel") QwenChatModel complexModel,
                          QwenEmbeddingModel embeddingModel,
                          EmbeddingStore<TextSegment> embeddingStore) {
        this.complexModel = complexModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Override
    public String execute(String userInput, String sessionId) {
        log.info("CultureRAGTool执行 - 使用复杂模型(qwen3-235b-a22b)处理文化问题");
        
        Embedding query = embeddingModel.embed(userInput).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(query, 5, 0.6);

        double relevance = calculateRelevanceScore(matches);
        log.info("RAG查询 - 会话ID: {}, 用户输入: {}, 检索文档数: {}, 相关性分数: {}",
                sessionId, userInput, matches.size(), String.format("%.3f", relevance));

        String context = matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n\n"));

        String prompt = """
                作为一位博物馆专业导览员，请基于以下上下文信息深入回答用户问题。
                
                上下文信息：
                %s
                
                用户问题：%s
                
                请你：
                1. 基于上下文信息提供准确、详细、富有洞察力的回答
                2. 如果可能，提供相关的历史背景和文化意义
                3. 用专业但易懂的语言解释复杂概念
                4. 如果上下文中没有足够相关信息，请诚实说明并提供你所了解的基础知识
                """.formatted(context, userInput);

        return complexModel.generate(prompt);
    }

    private double calculateRelevanceScore(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches == null || matches.isEmpty()) {
            return 0.0;
        }
        double total = matches.stream().mapToDouble(EmbeddingMatch::score).sum();
        return total / matches.size();
    }
}


