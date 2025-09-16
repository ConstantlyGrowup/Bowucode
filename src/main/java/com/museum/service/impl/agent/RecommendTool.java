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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    public String execute(String userInput, String userId, String conversationContext) {
        log.info("RecommendTool执行 - 使用复杂模型(qwen3-235b-a22b)处理推荐问题，用户ID: {}", userId);
        
        // 1. 获取用户历史观展记录
        QueryWrapper<MsReserveDetail> qw = new QueryWrapper<>();
        qw.lambda().eq(MsReserveDetail::getUserId, userId).eq(MsReserveDetail::getVldStat, "1");
        List<MsReserveDetail> details = reserveDetialMapper.selectList(qw);

        List<Integer> reserveIds = details.stream().map(MsReserveDetail::getResId).collect(Collectors.toList());
        List<MsReserve> visited = reserveIds.isEmpty() ? List.of() : reserveMapper.selectBatchIds(reserveIds);

        String history = visited.stream()
                .map(ms -> String.format("《%s》- 日期: %s, 时间: %s, 描述: %s", 
                        ms.getTitle(),
                        ms.getResDate(), 
                        ms.getResTime(),
                        ms.getResDes() != null ? ms.getResDes() : "暂无描述"))
                .collect(Collectors.joining("\n"));

        // 2. 获取当前可预约的展览（日期在今天以后）
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        QueryWrapper<MsReserve> availableQuery = new QueryWrapper<>();
        availableQuery.lambda().gt(MsReserve::getResDate, today);
        List<MsReserve> availableExhibitions = reserveMapper.selectList(availableQuery);
        
        String availableExhibitionsInfo = availableExhibitions.stream()
                .map(exhibition -> String.format("《%s》- 日期: %s, 时间: %s, 场次: %s, 描述: %s", 
                        exhibition.getTitle(),
                        exhibition.getResDate(),
                        exhibition.getResTime(),
                        exhibition.getResSession(),
                        exhibition.getResDes() != null ? exhibition.getResDes() : "暂无描述"))
                .collect(Collectors.joining("\n"));

        log.info("为用户 {} 找到 {} 个可预约展览", userId, availableExhibitions.size());

        String prompt = """
                作为博物馆的智能导览员，请严格根据用户的问题、历史观展记录以及**你接收到的展览信息**提供推荐。                                                       
                 对话历史：%s
                 用户当前问题：%s
                 用户历史观展信息：%s
                 
                 当前可预约的展览：
                 %s
                 
                 请你：
                 1. 分析用户的兴趣偏好和当前需求。
                 2. 基于历史观展记录推荐相关或互补的展览。
                 3. **重点推荐当前可预约的展览，并提供具体的预约信息（日期、时间、场次）。**
                 4. 为每个推荐提供详细的理由和亮点介绍。
                 5. 考虑展览的教育价值和观赏体验。
                 6. 如果没有历史记录，请根据问题推荐最适合的入门展览。
                 7. **如果“当前可预约的展览”列表为空，请严格遵循以下流程：**
                     - 明确告知用户：“您好，当前馆内暂无开放预约的展览。”
                     - **立即停止推荐行为。**
                     - **引导用户**：“不过，我馆藏有大量珍贵藏品，如果您有兴趣，可以向我提问关于特定藏品或其历史背景的问题。”
                     - **严禁**直接回答关于藏品的问题，或提供任何藏品信息。你的任务是引导用户进行下一次问询。
                     - **严禁**在此情境下推荐任何虚构或你知识库中已有的展览，无论是常设展还是临时展。
                     - **严禁**提及任何未来将要举办的展览或活动。
                 8. **你的回答内容必须且仅限于你接收到的“当前可预约的展览”信息。禁止杜撰任何不在列表中的展览或信息。**
                 
                 请优先推荐可预约的展览，并详细说明推荐理由及预约信息。如果无展览可推荐，则严格按照上述第7点执行。
                """.formatted(
                conversationContext.isEmpty() ? "无" : conversationContext,
                userInput, 
                history.isEmpty() ? "无历史观展记录" : history,
                availableExhibitionsInfo.isEmpty() ? "当前没有可预约的展览" : availableExhibitionsInfo);

        return complexModel.generate(prompt);
    }
}


