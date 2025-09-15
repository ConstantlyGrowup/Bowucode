package com.museum.service.impl.agent;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CultureRAGTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(CultureRAGTool.class);

    private final QwenChatModel chatModel;
    private final QwenEmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public CultureRAGTool(QwenChatModel chatModel,
                          QwenEmbeddingModel embeddingModel,
                          EmbeddingStore<TextSegment> embeddingStore) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Override
    public String execute(String userInput, String sessionId) {
        Embedding query = embeddingModel.embed(userInput).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(query, 5, 0.6);

        double relevance = calculateRelevanceScore(matches);
        log.info("RAG查询 - 会话ID: {}, 用户输入: {}, 检索文档数: {}, 相关性分数: {}",
                sessionId, userInput, matches.size(), String.format("%.3f", relevance));

        String context = matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n\n"));

        String prompt = "基于以下上下文信息回答用户问题：\n\n" + context +
                "\n\n用户问题：" + userInput +
                "\n请基于上下文信息提供准确、详细的回答。如果上下文中没有相关信息，请说明。";

        return chatModel.generate(prompt);
    }

    private double calculateRelevanceScore(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches == null || matches.isEmpty()) {
            return 0.0;
        }
        double total = matches.stream().mapToDouble(EmbeddingMatch::score).sum();
        return total / matches.size();
    }
}


