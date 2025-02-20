package com.museum.controller;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.common.Message;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestParam String prompt,
            @RequestParam String messages) throws Exception {

        logger.info("Received GET chat request. Prompt: {}", prompt);

        return Flux.create(sink -> {  // 改用更底层的Flux.create
            try {
                List<Message> messageList = objectMapper.readValue(messages, new TypeReference<>() {});

                List<Message> fullMessages = new ArrayList<>();
                fullMessages.add(Message.builder().role("system").content("You are a helpful assistant.").build());
                fullMessages.addAll(messageList);

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
                                    }
                                },
                                sink::error,
                                () -> {
                                    // 当所有数据都发送完毕后，发送结束标志
                                    if (!sink.isCancelled()) {
                                        sink.next("[ovo-done]");
                                        sink.complete();
                                    }
                                }
                        );
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

}