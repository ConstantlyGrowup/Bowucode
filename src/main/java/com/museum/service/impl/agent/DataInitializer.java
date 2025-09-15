package com.museum.service.impl.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Resource
    private VectorSyncService vectorSyncService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            // 检查向量数据是否已初始化
            if (vectorSyncService.isVectorDataInitialized()) {
                log.info("向量数据已存在，跳过初始化");
                return;
            }

            log.info("开始初始化向量数据...");
            vectorSyncService.initializeVectorData();
            log.info("向量数据初始化完成");
        } catch (Exception e) {
            log.warn("向量数据初始化失败: {}", e.getMessage(), e);
        }
    }
}
