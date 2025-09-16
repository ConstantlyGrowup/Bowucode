package com.museum.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Session工具类
 * 用于处理sessionID的生成和解析
 */
public class SessionUtils {
    
    private static final Logger log = LoggerFactory.getLogger(SessionUtils.class);
    
    /**
     * 从sessionID中解析出userID
     * sessionID格式：userID_randomString
     * 
     * @param sessionId 完整的sessionID
     * @return userID，如果解析失败返回null
     */
    public static String extractUserIdFromSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.warn("SessionID为空");
            return null;
        }
        
        try {
            // sessionID格式：userID_randomString
            String[] parts = sessionId.split("_");
            if (parts.length >= 2) {
                String userId = parts[0];
                log.debug("从sessionID [{}] 中解析出userID: {}", sessionId, userId);
                return userId;
            } else {
                log.warn("SessionID格式不正确，无法解析userID: {}", sessionId);
                return null;
            }
        } catch (Exception e) {
            log.error("解析sessionID失败: {}, 错误: {}", sessionId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证sessionID格式是否正确
     * 
     * @param sessionId 要验证的sessionID
     * @return 是否格式正确
     */
    public static boolean isValidSessionIdFormat(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = sessionId.split("_");
        // 至少包含userID和随机字符串两部分
        return parts.length >= 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty();
    }
    
    /**
     * 获取sessionID中的随机部分
     * 
     * @param sessionId 完整的sessionID
     * @return 随机字符串部分
     */
    public static String extractRandomPartFromSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = sessionId.split("_");
        if (parts.length >= 2) {
            return parts[1];
        }
        return null;
    }
}
