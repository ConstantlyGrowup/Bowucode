package com.museum.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 根据实际情况调整
        executor.setMaxPoolSize(20); // 根据实际情况调整
        executor.setQueueCapacity(100); // 根据实际情况调整
        executor.initialize();
        return executor;
    }
}