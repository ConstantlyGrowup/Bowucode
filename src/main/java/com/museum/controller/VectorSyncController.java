package com.museum.controller;

import com.museum.service.impl.agent.VectorSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/vector-sync")
public class VectorSyncController {

    @Resource
    private VectorSyncService vectorSyncService;

    /**
     * 检查向量数据库状态
     */
    @GetMapping("/status")
    public ResponseEntity<String> checkStatus() {
        boolean initialized = vectorSyncService.isVectorDataInitialized();
        return ResponseEntity.ok("向量数据库状态: " + (initialized ? "已初始化" : "未初始化"));
    }

    /**
     * 手动触发向量数据同步
     */
    @PostMapping("/sync")
    public ResponseEntity<String> syncData() {
        try {
            vectorSyncService.syncAllCollections();
            return ResponseEntity.ok("向量数据同步完成");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("向量数据同步失败: " + e.getMessage());
        }
    }
}
