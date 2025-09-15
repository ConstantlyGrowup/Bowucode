package com.museum.domain.chato;

import com.alibaba.dashscope.common.Message;
import com.museum.constants.Constant;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatSessionManager {
    private static final ConcurrentHashMap<String, List<Message>> sessions = new ConcurrentHashMap<>();
    public static void addMessage(String sessionId, Message message) {
        sessions.compute(sessionId, (key, messages) -> {
            if (messages == null) {
                messages = new CopyOnWriteArrayList<>();
                messages.add(Message.builder().role("system").content(Constant.CONSTRAIN_SYSTEM_ROLE_new).build());
                messages.add(Message.builder().role("assistant").content(Constant.CONSTRAIN_ASSISTANT_WELCOME).build());
            }
            messages.add(message);
            return messages;
        });
    }

    public static List<Message> getMessages(String sessionId) {
        return sessions.getOrDefault(sessionId, List.of());
    }
}