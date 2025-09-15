package com.museum.service.impl.agent;

import com.museum.domain.enums.IntentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GuideAgentService {

    private static final Logger log = LoggerFactory.getLogger(GuideAgentService.class);
    
    private final IntentClassifierService intentClassifierService;
    private final IntentRouterImpl intentRouter;

    public GuideAgentService(IntentClassifierService intentClassifierService,
                             IntentRouterImpl intentRouter) {
        this.intentClassifierService = intentClassifierService;
        this.intentRouter = intentRouter;
    }

    public String processMessage(String message, String sessionId) {
        log.info("开始处理用户消息 - 会话ID: {}, 消息: '{}'", sessionId, message);
        
        // 意图分类
        IntentType intent = intentClassifierService.classify(message);
        log.info("意图分类完成 - 会话ID: {}, 分类结果: {}", sessionId, intent);
        
        // 路由到对应工具
        log.debug("开始路由到对应工具 - 意图: {}", intent);
        String response = intentRouter.route(intent, message, sessionId);
        
        log.info("消息处理完成 - 会话ID: {}, 响应长度: {} 字符", sessionId, response.length());
        log.debug("完整响应内容: {}", response);
        
        return response;
    }
}


