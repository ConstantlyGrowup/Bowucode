package com.museum.service.impl.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.museum.domain.po.MsAnnouncement;
import com.museum.mapper.MsAnnouncementMapper;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RulesQATool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(RulesQATool.class);
    private final QwenChatModel simpleModel;
    private final MsAnnouncementMapper msAnnouncementMapper;

    public RulesQATool(@Qualifier("simpleQwenChatModel") QwenChatModel simpleModel, MsAnnouncementMapper msAnnouncementMapper) {
        this.simpleModel = simpleModel;
        this.msAnnouncementMapper = msAnnouncementMapper;
    }


    @Override
    public String execute(String userInput, String userId, String conversationContext) {
        log.info("RulesQATool执行 - 使用简单模型(qwen-plus)处理规则问题，用户ID: {}", userId);
        //查询数据库得到近期所有公告
        QueryWrapper<MsAnnouncement> wrapper = new QueryWrapper<>();
        wrapper.lambda().isNotNull(MsAnnouncement::getContentText);
        List<MsAnnouncement> announcements = msAnnouncementMapper.selectList(wrapper);
        String announcementContent = announcements.stream()
                .map(MsAnnouncement::getContentText)
                .collect(Collectors.joining("\n"));
        String prompt = """
        1. 角色
        你是一个专业的、精通文旅平台所有规章制度的问答导览僧。
        2. 任务
        根据用户关于馆内规章制度、规则的提问，直接且准确地从提供的参考规则文本中提取信息，并进行清晰的回答。
        2. 约束
        信息源: 所有回答必须严格基于提供的馆内规章制度文本。
        准确性: 你的回答必须是公告内容的准确复述或提炼，不允许任何形式的个人解读或信息添加。
        简洁明了: 直接回答问题，避免使用复杂的语言或与问题无关的额外信息。
        对话上下文: 如果有之前的对话记录，可以参考但不要重复之前已回答的内容。
        5. 输出
        最终的回答将是公告内容的直接引用、提炼或转述，用于回答用户的具体规则问题。

        对话历史：%s
        公告内容：%s,注意新开展览的公告
        用户问题：%s
        """;
        return simpleModel.generate(prompt.formatted(
                conversationContext.isEmpty() ? "无" : conversationContext,
                announcementContent, 
                userInput));
    }
}


