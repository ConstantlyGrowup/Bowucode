package com.museum.service.impl.agent;

import com.museum.constants.Constant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.stereotype.Component;

@Component
public class RulesQATool implements Tool {

    private final QwenChatModel chatModel;

    public RulesQATool(QwenChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String execute(String userInput, String sessionId) {
        String prompt = Constant.CONSTRAIN_SYSTEM_ROLE_new + "\n\n用户问题：" + userInput + "\n请基于以上规则知识，给出简洁准确的回答。";
        return chatModel.generate(prompt);
    }
}


