package com.museum.service.impl.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.museum.domain.po.MsReserve;
import com.museum.domain.po.MsReserveDetail;
import com.museum.mapper.ReserveDetialMapper;
import com.museum.mapper.ReserveMapper;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecommendTool implements Tool {

    private final ReserveDetialMapper reserveDetialMapper;
    private final ReserveMapper reserveMapper;
    private final QwenChatModel chatModel;

    public RecommendTool(ReserveDetialMapper reserveDetialMapper,
                         ReserveMapper reserveMapper,
                         QwenChatModel chatModel) {
        this.reserveDetialMapper = reserveDetialMapper;
        this.reserveMapper = reserveMapper;
        this.chatModel = chatModel;
    }

    @Override
    public String execute(String userInput, String sessionId) {
        // 这里将 sessionId 视作 userId 使用（如需不同来源，可在上层传入 userId）
        QueryWrapper<MsReserveDetail> qw = new QueryWrapper<>();
        qw.lambda().eq(MsReserveDetail::getUserId, sessionId).eq(MsReserveDetail::getVldStat, "1");
        List<MsReserveDetail> details = reserveDetialMapper.selectList(qw);

        List<Integer> reserveIds = details.stream().map(MsReserveDetail::getResId).collect(Collectors.toList());
        List<MsReserve> visited = reserveIds.isEmpty() ? List.of() : reserveMapper.selectBatchIds(reserveIds);

        String history = visited.stream()
                .map(ms -> ms.getTitle() + "(" + ms.getResDate() + " " + ms.getResTime() + ")")
                .collect(Collectors.joining("; "));

        String prompt = "用户历史观展信息：" + (history.isEmpty() ? "无" : history) + "\n" +
                "请基于以上信息，推荐3个可能感兴趣的展览或藏品，给出简短理由。如果没有检索到展览，禁止给用户推荐展览。告诉用户当前馆内没有展览，并且询问是否需要了解馆内藏品";

        return chatModel.generate(prompt);
    }
}


