package com.museum.controller;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.common.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.museum.domain.chato.ChatSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
//todo:雪花算法生成uuid+缓存持久化聊天记录
@RestController
@RequestMapping("/ai")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestParam String prompt,
            @RequestParam String sessionId) throws Exception {

        logger.info("Received GET chat request. Prompt: {}. SessionId: {}", prompt, sessionId);

        // 将用户的问题加入到对应的session中
        ChatSessionManager.addMessage(sessionId, Message.builder().role("user").content(prompt).build());
        logger.debug("User message added to session [{}]: {}", sessionId, prompt);

        return Flux.create(sink -> {
            StringBuilder accumulatedResponse = new StringBuilder();  // 累积AI助手的回复

            try {
                List<Message> fullMessages = ChatSessionManager.getMessages(sessionId);
                logger.debug("Full messages for session [{}]: {}", sessionId, fullMessages);

                ApplicationParam param = ApplicationParam.builder()
                        .apiKey("sk-b4ca1a8b48734565907af761fb97c31d")
                        .appId("50caa8cd9ea148d0bf2339a65acebd44")
                        .prompt(prompt)
                        .messages(fullMessages)
                        .incrementalOutput(true)
                        .build();

                Application application = new Application();
                application.streamCall(param)
                        .subscribe(
                                data -> {
                                    String chunk = data.getOutput().getText();
                                    if (!sink.isCancelled()) {
                                        sink.next(chunk);
                                        accumulatedResponse.append(chunk);  // 累积回复
                                    }
                                },
                                error -> {
                                    logger.error("Error processing stream for session [{}]: {}", sessionId, error.getMessage());
                                    sink.error(error);
                                },
                                () -> {
                                    if (!sink.isCancelled()) {
                                        sink.next("[ovo-done]");
                                        // 在流结束时将完整的AI回复也加入到session中
                                        ChatSessionManager.addMessage(sessionId, Message.builder().role("assistant").content(accumulatedResponse.toString()).build());
                                        logger.debug("Assistant response added to session [{}]: {}", sessionId, accumulatedResponse.toString());
                                        sink.complete();
                                    }
                                }
                        );
            } catch (Exception e) {
                logger.error("Unexpected error for session [{}]: {}", sessionId, e.getMessage());
                sink.error(e);
            }
        });
    }
}