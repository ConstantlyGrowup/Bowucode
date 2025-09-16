package com.museum.service.impl.agent;

import com.alibaba.dashscope.common.Message;
import com.museum.constants.Constant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 导览僧对话上下文管理器
 * 仅维护当前会话的临时对话记录，支持自动摘要
 */
@Service
public class ConversationManager {

    private static final Logger log = LoggerFactory.getLogger(ConversationManager.class);

    // 内存中的会话缓存，仅用于当前会话
    private static final ConcurrentHashMap<String, List<Message>> sessionCache = new ConcurrentHashMap<>();
    
    // 对话轮次阈值
    private static final int MAX_CONVERSATION_TURNS = 10;

    @Resource
    @Qualifier("simpleQwenChatModel")
    private QwenChatModel chatModel;

    /**
     * 添加消息到会话
     */
    public void addMessage(String sessionId, String role, String content) {
        log.debug("添加消息到会话 [{}] - 角色: {}", sessionId, role);
        
        Message message = Message.builder()
                .role(role)
                .content(content)
                .build();

        // 更新内存缓存
        List<Message> messages = sessionCache.computeIfAbsent(sessionId, k -> {
            List<Message> newMessages = new CopyOnWriteArrayList<>();
            // 初始化系统提示
            newMessages.add(Message.builder()
                    .role("system")
                    .content(Constant.CONSTRAIN_SYSTEM_ROLE_new)
                    .build());
            return newMessages;
        });
        
        messages.add(message);

        // 检查是否需要摘要
        if (countUserMessages(messages) >= MAX_CONVERSATION_TURNS) {
            log.info("会话 [{}] 达到 {} 轮对话，触发摘要", sessionId, MAX_CONVERSATION_TURNS);
            summarizeConversationAsync(sessionId);
        }
    }

    /**
     * 获取会话消息列表
     */
    public List<Message> getMessages(String sessionId) {
        List<Message> messages = sessionCache.get(sessionId);
        if (messages != null) {
            log.debug("获取会话 [{}] 消息，共 {} 条", sessionId, messages.size());
            return new ArrayList<>(messages);
        }

        // 创建新会话
        log.debug("创建新会话 [{}]", sessionId);
        List<Message> newMessages = new CopyOnWriteArrayList<>();
        newMessages.add(Message.builder()
                .role("system")
                .content(Constant.CONSTRAIN_SYSTEM_ROLE_new)
                .build());
        sessionCache.put(sessionId, newMessages);
        return new ArrayList<>(newMessages);
    }

    /**
     * 获取会话上下文（用于传递给各个工具）
     */
    public String getConversationContext(String sessionId) {
        List<Message> messages = getMessages(sessionId);
        if (messages.size() <= 1) { // 只有系统消息
            return "";
        }

        // 提取最近的对话内容作为上下文
        return messages.stream()
                .filter(msg -> !"system".equals(msg.getRole()))
                .limit(6) // 最近3轮对话
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));
    }

    /**
     * 异步摘要对话
     */
    @Async
    public void summarizeConversationAsync(String sessionId) {
        try {
            summarizeConversation(sessionId);
        } catch (Exception e) {
            log.error("摘要会话 [{}] 失败: {}", sessionId, e.getMessage(), e);
        }
    }

    /**
     * 摘要对话
     */
    private void summarizeConversation(String sessionId) {
        List<Message> messages = sessionCache.get(sessionId);
        if (messages == null || messages.size() <= 3) { // 系统消息 + 少量用户消息
            log.debug("会话 [{}] 消息过少，跳过摘要", sessionId);
            return;
        }

        log.info("开始摘要会话 [{}]，共 {} 条消息", sessionId, messages.size());

        // 生成摘要
        String summary = generateSummary(messages);
        if (summary == null || summary.trim().isEmpty()) {
            log.warn("会话 [{}] 摘要生成失败", sessionId);
            return;
        }

        // 清理旧消息，保留系统消息和摘要
        cleanupOldMessages(sessionId, summary);

        log.info("会话 [{}] 摘要完成", sessionId);
    }

    /**
     * 生成对话摘要
     */
    private String generateSummary(List<Message> messages) {
        try {
            String conversationText = messages.stream()
                    .filter(msg -> !"system".equals(msg.getRole()))
                    .map(msg -> msg.getRole() + ": " + msg.getContent())
                    .collect(Collectors.joining("\n"));

            String prompt = String.format("""
                请对以下对话进行摘要，提取关键信息和用户兴趣点：
                
                对话内容：
                %s
                
                要求：
                1. 保留用户主要询问的文物类型、朝代、展厅等关键信息
                2. 记录用户的兴趣偏好
                3. 简洁明了，控制在200字以内
                4. 格式：用户关心的主要内容包括...
                """, conversationText);

            String summary = chatModel.generate(prompt);
            log.debug("生成摘要: {}", summary);
            return summary;
        } catch (Exception e) {
            log.error("生成摘要失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 清理旧消息
     */
    private void cleanupOldMessages(String sessionId, String summary) {
        List<Message> newMessages = new CopyOnWriteArrayList<>();
        
        // 保留系统消息
        newMessages.add(Message.builder()
                .role("system")
                .content(Constant.CONSTRAIN_SYSTEM_ROLE_new)
                .build());
        
        // 添加摘要作为系统消息
        newMessages.add(Message.builder()
                .role("system")
                .content("对话历史摘要：" + summary)
                .build());
        
        // 保留最近的3轮对话（6条消息）
        List<Message> currentMessages = sessionCache.get(sessionId);
        if (currentMessages != null) {
            List<Message> recentMessages = currentMessages.stream()
                    .filter(msg -> !"system".equals(msg.getRole()))
                    .skip(Math.max(0, currentMessages.size() - 6))
                    .collect(Collectors.toList());
            
            newMessages.addAll(recentMessages);
        }
        
        // 更新缓存
        sessionCache.put(sessionId, newMessages);
        
        log.info("会话 [{}] 消息清理完成，保留 {} 条消息", sessionId, newMessages.size());
    }

    /**
     * 统计用户消息数量
     */
    private long countUserMessages(List<Message> messages) {
        return messages.stream()
                .filter(msg -> "user".equals(msg.getRole()))
                .count();
    }

    /**
     * 清理会话（可用于测试或管理）
     */
    public void clearSession(String sessionId) {
        sessionCache.remove(sessionId);
        log.info("已清理会话 [{}]", sessionId);
    }

    /**
     * 获取当前活跃会话数量
     */
    public int getActiveSessionCount() {
        return sessionCache.size();
    }
}