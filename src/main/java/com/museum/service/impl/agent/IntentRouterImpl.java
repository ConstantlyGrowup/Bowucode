package com.museum.service.impl.agent;

import com.museum.domain.enums.IntentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class IntentRouterImpl {

    private static final Logger log = LoggerFactory.getLogger(IntentRouterImpl.class);
    
    private final Map<IntentType, Tool> toolMap = new EnumMap<>(IntentType.class);

    public IntentRouterImpl(RulesQATool rulesQATool,
                            CultureRAGTool cultureRAGTool,
                            RecommendTool recommendTool) {
        toolMap.put(IntentType.RULES, rulesQATool);
        toolMap.put(IntentType.CULTURE, cultureRAGTool);
        toolMap.put(IntentType.RECOMMEND, recommendTool);
        
        log.info("意图路由器初始化完成 - 已注册工具: {}", toolMap.keySet());
    }

    public String route(IntentType intentType, String userInput, String sessionId) {
        Tool tool = toolMap.getOrDefault(intentType, toolMap.get(IntentType.RULES));
        String toolName = tool.getClass().getSimpleName();
        
        log.info("路由决策 - 意图: {} -> 工具: {}", intentType, toolName);
        log.debug("执行工具 - 工具: {}, 用户输入: '{}', 会话ID: {}", toolName, userInput, sessionId);
        
        String result = tool.execute(userInput, sessionId);
        
        log.info("工具执行完成 - 工具: {}, 响应长度: {} 字符", toolName, result.length());
        
        return result;
    }
}


