package com.museum.service.impl.agent;

import com.museum.domain.enums.IntentType;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class IntentClassifierServiceImpl implements IntentClassifierService {

    private static final Logger log = LoggerFactory.getLogger(IntentClassifierServiceImpl.class);
    private final QwenChatModel simpleModel;

    public IntentClassifierServiceImpl(@Qualifier("simpleQwenChatModel") QwenChatModel simpleModel) {
        this.simpleModel = simpleModel;
    }

    @Override
    public IntentType classify(String userInput) {
        String prompt = buildClassificationPrompt(userInput);
        
        log.debug("意图分类 - 用户输入: {}", userInput);
        log.debug("意图分类 - 发送给LLM的prompt: {}", prompt);
        
        String result = simpleModel.generate(prompt);
        String normalized = result.trim().toUpperCase();
        
        log.debug("意图分类 - LLM原始回复: {}", result);
        log.debug("意图分类 - 标准化后: {}", normalized);
        
        IntentType intentType = parseIntentFromResponse(normalized);
        log.info("意图分类结果 - 用户输入: '{}' -> 意图: {}", userInput, intentType);
        
        return intentType;
    }
    
    private String buildClassificationPrompt(String userInput) {
        return """
                请判断以下用户输入的意图类型，严格按照以下规则分类：
                
                ## 分类规则：
                
                **RULES** - 规章制度和基本信息查询：
                - 博物馆开放时间、闭馆时间
                - 门票价格、购票方式
                - 参观规则、注意事项
                - 地址、交通、停车
                - 联系方式、客服
                - 拍照规定、禁止事项
                - 导览服务、讲解时间
                
                **CULTURE** - 文化背景和藏品知识：
                - 藏品历史、文化背景
                - 文物介绍、制作工艺
                - 朝代历史、文化故事
                - 艺术价值、收藏意义
                - 考古发现、文物保护
                - 传统文化、民俗风情
                
                **RECOMMEND** - 推荐和建议：
                - 推荐展览、展品
                - 参观路线建议
                - 必看藏品推荐
                - 适合特定人群的展览
                - 个性化推荐
                
                ## 用户输入：
                """ + userInput + """
                
                ## 要求：
                请仅返回以下三个选项之一，不要包含其他内容：
                RULES
                CULTURE  
                RECOMMEND
                """;
    }
    
    private IntentType parseIntentFromResponse(String normalized) {
        // 更精确的匹配逻辑
        if (normalized.contains("CULTURE")) {
            return IntentType.CULTURE;
        } else if (normalized.contains("RECOMMEND")) {
            return IntentType.RECOMMEND;
        } else if (normalized.contains("RULES")) {
            return IntentType.RULES;
        } else {
            // 如果都不匹配，根据关键词进行二次判断

                log.debug("无法明确分类，默认返回RULES意图");
                return IntentType.RULES;
            }
        }
    }



