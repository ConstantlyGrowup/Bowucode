package com.museum.domain.chato;

import com.alibaba.dashscope.common.Message;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatSessionManager {
    private static final ConcurrentHashMap<String, List<Message>> sessions = new ConcurrentHashMap<>();

    public static void addMessage(String sessionId, Message message) {
        sessions.compute(sessionId, (key, messages) -> {
            if (messages == null) {
                messages = new CopyOnWriteArrayList<>();
                messages.add(Message.builder().role("system").content("You are a helpful assistant.").build());
            }
            messages.add(message);
            return messages;
        });
    }

    public static List<Message> getMessages(String sessionId) {
        return sessions.getOrDefault(sessionId, List.of());
    }
}