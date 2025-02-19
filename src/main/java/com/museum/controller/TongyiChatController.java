package com.museum.controller;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import io.reactivex.Flowable;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/chat")
public class TongyiChatController {
    private static final Logger logger = LoggerFactory.getLogger(TongyiChatController.class);
    private static List<Message> messages = new ArrayList<>();
    private static StringBuilder fullContent = new StringBuilder();
    private final Generation gen;

    public TongyiChatController() {
        this.gen = new Generation();
        messages.add(createMessage(Role.SYSTEM, "You are a helpful assistant."));
    }

    @PostMapping("/send")
    public String sendMessage(@RequestBody String userInput) {
        try {
            if ("exit".equalsIgnoreCase(userInput)) {
                return "Conversation ended.";
            }
            messages.add(createMessage(Role.USER, userInput));
            String response = streamCallWithMessages();
            logger.info("User input: {}", userInput);
            logger.info("AI response: {}", response);
            return response;
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            logger.error("Error during chat: ", e);
            return "Error: " + e.getMessage();
        }
    }

    private String streamCallWithMessages() throws NoApiKeyException, ApiException, InputRequiredException {
        GenerationParam param = createGenerationParam(messages);
        StringBuilder responseContent = new StringBuilder();
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingForEach(message -> {
            String content = message.getOutput().getChoices().get(0).getMessage().getContent();
            fullContent.append(content);
            responseContent.append(content).append("\n");
        });
        return responseContent.toString();
    }

    private GenerationParam createGenerationParam(List<Message> messages) {
        return GenerationParam.builder()
                .apiKey("sk-b4ca1a8b48734565907af761fb97c31d")
                .model("qwen-turbo")
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .incrementalOutput(true)
                .build();
    }

    private static Message createMessage(Role role, String content) {
        return Message.builder().role(role.getValue()).content(content).build();
    }
}