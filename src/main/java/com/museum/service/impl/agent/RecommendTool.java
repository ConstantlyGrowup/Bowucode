package com.museum.service.impl.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.museum.domain.po.MsReserve;
import com.museum.domain.po.MsReserveDetail;
import com.museum.mapper.ReserveDetialMapper;
import com.museum.mapper.ReserveMapper;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecommendTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(RecommendTool.class);
    
    private final ReserveDetialMapper reserveDetialMapper;
    private final ReserveMapper reserveMapper;
    private final QwenChatModel complexModel;

    public RecommendTool(ReserveDetialMapper reserveDetialMapper,
                         ReserveMapper reserveMapper,
                         @Qualifier("complexQwenChatModel") QwenChatModel complexModel) {
        this.reserveDetialMapper = reserveDetialMapper;
        this.reserveMapper = reserveMapper;
        this.complexModel = complexModel;
    }

    @Override
    public String execute(String userInput, String sessionId) {
        log.info("RecommendTool执行 - 使用复杂模型(qwen3-235b-a22b)处理推荐问题");
        
        // 这里将 sessionId 视作 userId 使用（如需不同来源，可在上层传入 userId）
        QueryWrapper<MsReserveDetail> qw = new QueryWrapper<>();
        qw.lambda().eq(MsReserveDetail::getUserId, sessionId).eq(MsReserveDetail::getVldStat, "1");
        List<MsReserveDetail> details = reserveDetialMapper.selectList(qw);

        List<Integer> reserveIds = details.stream().map(MsReserveDetail::getResId).collect(Collectors.toList());
        List<MsReserve> visited = reserveIds.isEmpty() ? List.of() : reserveMapper.selectBatchIds(reserveIds);

        String history = visited.stream()
                .map(ms -> ms.getTitle() + "(" + ms.getResDate() + " " + ms.getResTime() + ")")
                .collect(Collectors.joining("; "));

        String prompt = """
                作为博物馆的智能导览员，请根据用户的需求和历史观展记录提供个性化推荐。
                
                用户当前问题：%s
                
                用户历史观展信息：%s
                
                请你：
                1. 分析用户的兴趣偏好和当前需求
                2. 基于历史观展记录推荐相关或互补的展览/藏品
                3. 为每个推荐提供详细的理由和亮点介绍
                4. 考虑展览的教育价值和观赏体验
                5. 如果没有历史记录，请根据问题推荐最适合的入门展览
                6. 如果没有检索到展览，请告诉用户当前馆内没有展览，并询问是否需要了解馆内藏品
                
                请推荐3-5个展览或藏品，并详细说明推荐理由。
                """.formatted(userInput, history.isEmpty() ? "无历史观展记录" : history);

        return complexModel.generate(prompt);
    }
}


