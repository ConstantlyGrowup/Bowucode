package com.museum.service.impl.agent;

public interface Tool {
    /**
     * 执行工具功能
     * 
     * @param userInput 用户输入
     * @param userId 用户ID（从sessionID中解析出来）
     * @param conversationContext 对话上下文
     * @return 工具执行结果
     */
    String execute(String userInput, String userId, String conversationContext);
}


