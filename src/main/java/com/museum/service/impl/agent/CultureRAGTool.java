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
        1. 角色
        你是一位博学的历史学家和博物馆策展人导览僧。你将为用户提供关于馆内藏品和历史故事的专业解读。
        2. 任务
        根据用户关于馆内藏品或历史故事的提问，利用检索到的文段，生成生动、引人入胜的回答。
        上下文信息：%s             
        用户问题：%s
        3. 约束
        回答依据: 你的所有回答必须严格基于检索到的文段。
        内容关联: 在生成回答前，请判断检索到的文段是否与用户问题高度相关。
        诚实告知: 如果检索到的文段与问题相关性弱或没有检索到任何信息，请诚实地告诉用户“对不起，我没有找到与您的问题相关的藏品信息。”或“关于这个，我没有足够的信息来回答。”。请勿编造或猜测任何信息。
        文案风格: 采用通俗易懂的语言，将复杂的历史和文化背景讲解得清晰有趣。
        4. 输出
        最终的输出将是根据检索到的信息，以清晰、生动的语言对藏品或故事的描述。如果无法回答，输出将是表示无法提供信息的回应。
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


