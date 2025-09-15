package com.museum.service.impl.agent;

import com.museum.constants.Constant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class RulesQATool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(RulesQATool.class);
    private final QwenChatModel simpleModel;

    public RulesQATool(@Qualifier("simpleQwenChatModel") QwenChatModel simpleModel) {
        this.simpleModel = simpleModel;
    }

    @Override
    public String execute(String userInput, String sessionId) {
        log.info("RulesQATool执行 - 使用简单模型(qwen-plus)处理规则问题");
        String prompt = Constant.CONSTRAIN_SYSTEM_ROLE_new + "\n\n用户问题：" + userInput + "\n请基于以上规则知识，给出简洁准确的回答。";
        return simpleModel.generate(prompt);
    }
}


