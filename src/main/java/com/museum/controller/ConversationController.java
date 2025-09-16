package com.museum.controller;

import com.museum.service.impl.agent.ConversationManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 对话管理控制器
 * 用于测试和管理对话上下文
 */
@RestController
@RequestMapping("/conversation")
public class ConversationController {

    @Resource
    private ConversationManager conversationManager;

    /**
     * 获取会话上下文信息
     */
    @GetMapping("/context/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionContext(@PathVariable String sessionId) {
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("conversationContext", conversationManager.getConversationContext(sessionId));
        result.put("messages", conversationManager.getMessages(sessionId));
        return ResponseEntity.ok(result);
    }

    /**
     * 清理指定会话
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<String> clearSession(@PathVariable String sessionId) {
        conversationManager.clearSession(sessionId);
        return ResponseEntity.ok("会话 " + sessionId + " 已清理");
    }

    /**
     * 获取系统状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("activeSessionCount", conversationManager.getActiveSessionCount());
        result.put("status", "running");
        return ResponseEntity.ok(result);
    }

    /**
     * 手动触发会话摘要（用于测试）
     */
    @PostMapping("/summarize/{sessionId}")
    public ResponseEntity<String> triggerSummary(@PathVariable String sessionId) {
        conversationManager.summarizeConversationAsync(sessionId);
        return ResponseEntity.ok("已触发会话 " + sessionId + " 的摘要任务");
    }
}
