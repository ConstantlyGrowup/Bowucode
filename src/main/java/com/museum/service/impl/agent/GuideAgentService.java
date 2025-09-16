package com.museum.service.impl.agent;

import com.museum.domain.enums.IntentType;
import com.museum.utils.SessionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class GuideAgentService {

    private static final Logger log = LoggerFactory.getLogger(GuideAgentService.class);
    
    private final IntentClassifierService intentClassifierService;
    private final IntentRouterImpl intentRouter;
    
    @Resource
    private ConversationManager conversationManager;

    public GuideAgentService(IntentClassifierService intentClassifierService,
                             IntentRouterImpl intentRouter) {
        this.intentClassifierService = intentClassifierService;
        this.intentRouter = intentRouter;
    }

    public String processMessage(String message, String sessionId) {
        log.info("开始处理用户消息 - 会话ID: {}, 消息: '{}'", sessionId, message);
        
        // 0. 验证并解析sessionID
        if (!SessionUtils.isValidSessionIdFormat(sessionId)) {
            log.warn("SessionID格式不正确: {}", sessionId);
            return "抱歉，会话信息有误，请刷新页面重试。";
        }
        
        String userId = SessionUtils.extractUserIdFromSessionId(sessionId);
        if (userId == null) {
            log.warn("无法从sessionID中解析出userID: {}", sessionId);
            return "抱歉，无法识别用户身份，请重新登录。";
        }
        
        log.info("从sessionID解析出userID: {} (会话: {})", userId, sessionId);
        
        // 1. 添加用户消息到对话历史
        conversationManager.addMessage(sessionId, "user", message);
        
        // 2. 意图分类
        IntentType intent = intentClassifierService.classify(message);
        log.info("意图分类完成 - 用户: {}, 会话: {}, 分类结果: {}", userId, sessionId, intent);
        
        // 3. 获取对话上下文
        String conversationContext = conversationManager.getConversationContext(sessionId);
        
        // 4. 路由到对应工具（传递userId而不是sessionId）
        log.debug("开始路由到对应工具 - 意图: {}, 用户: {}", intent, userId);
        String response = intentRouter.route(intent, message, userId, conversationContext);
        
        // 5. 添加助手回复到对话历史
        conversationManager.addMessage(sessionId, "assistant", response);
        
        log.info("消息处理完成 - 用户: {}, 会话: {}, 响应长度: {} 字符", userId, sessionId, response.length());
        log.debug("完整响应内容: {}", response);
        
        return response;
    }
}


