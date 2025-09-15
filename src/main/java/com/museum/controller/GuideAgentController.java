package com.museum.controller;

import com.museum.service.impl.agent.GuideAgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/guide-agent")
public class GuideAgentController {

    @Resource
    private GuideAgentService guideAgentService;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam("message") String message,
                                       @RequestParam("sessionId") String sessionId) {
        String answer = guideAgentService.processMessage(message, sessionId);
        return ResponseEntity.ok(answer);
    }
}


