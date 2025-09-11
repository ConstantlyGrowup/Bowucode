package com.museum.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.museum.domain.po.NewsBatch;
import com.museum.domain.po.NewsItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 新闻服务实现类
 * </p>
 *
 * @author museum
 * @since 2025-01-11
 */
@Slf4j
@Service
public class NewsService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Redis key前缀
    private static final String NEWS_BATCH_KEY_PREFIX = "news:batch:";
    private static final String NEWS_BATCHES_KEY = "news:batches";
    private static final String NEWS_LATEST_KEY = "news:latest";
    
    // 缓存过期时间：7天
    private static final long CACHE_EXPIRE_DAYS = 7;

    /**
     * 处理并存储新闻数据到Redis
     * @param newsBatches 新闻批次数据
     */
    public void processAndStoreNews(List<NewsBatch> newsBatches) {
        if (CollUtil.isEmpty(newsBatches)) {
            log.warn("新闻批次数据为空，跳过处理");
            return;
        }

        for (NewsBatch batch : newsBatches) {
            if (batch == null || StrUtil.isBlank(batch.getBatchId())) {
                log.warn("批次数据无效，跳过处理: {}", batch);
                continue;
            }

            try {
                // 存储单个批次数据
                storeNewsBatch(batch);
                
                // 更新批次列表
                updateBatchList(batch.getBatchId());
                
                log.info("成功存储新闻批次: {}", batch.getBatchId());
            } catch (Exception e) {
                log.error("存储新闻批次失败: {}", batch.getBatchId(), e);
                throw new RuntimeException("存储新闻批次失败: " + batch.getBatchId(), e);
            }
        }

        // 更新最新新闻缓存
        updateLatestNewsCache();
        
        log.info("所有新闻批次处理完成，共处理 {} 个批次", newsBatches.size());
    }

    /**
     * 存储单个新闻批次到Redis
     * @param batch 新闻批次
     */
    private void storeNewsBatch(NewsBatch batch) {
        String batchKey = NEWS_BATCH_KEY_PREFIX + batch.getBatchId();
        
        // 将批次数据序列化为JSON并存储
        String batchJson = JSONUtil.toJsonStr(batch);
        stringRedisTemplate.opsForValue().set(batchKey, batchJson, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        
        log.debug("存储新闻批次到Redis: {}", batchKey);
    }

    /**
     * 更新批次列表
     * @param batchId 批次ID
     */
    private void updateBatchList(String batchId) {
        // 将批次ID添加到批次列表中
        stringRedisTemplate.opsForSet().add(NEWS_BATCHES_KEY, batchId);
        
        // 设置批次列表的过期时间
        stringRedisTemplate.expire(NEWS_BATCHES_KEY, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        
        log.debug("更新批次列表: {}", batchId);
    }

    /**
     * 更新最新新闻缓存
     */
    private void updateLatestNewsCache() {
        try {
            // 获取所有批次ID
            Set<String> batchIds = stringRedisTemplate.opsForSet().members(NEWS_BATCHES_KEY);
            if (CollUtil.isEmpty(batchIds)) {
                log.warn("没有找到任何新闻批次");
                return;
            }

            // 获取最新的几个批次（最多5个）
            List<NewsBatch> latestBatches = new ArrayList<>();
            int count = 0;
            int maxBatches = 5;

            // 按批次ID排序获取最新的批次
            for (String batchId : batchIds) {
                if (count >= maxBatches) break;
                
                NewsBatch batch = getNewsBatchFromRedis(batchId);
                if (batch != null) {
                    latestBatches.add(batch);
                    count++;
                }
            }

            // 按批次时间排序（最新的在前）
            latestBatches.sort((b1, b2) -> {
                try {
                    LocalDateTime time1 = LocalDateTime.parse(b1.getBatchTime(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    LocalDateTime time2 = LocalDateTime.parse(b2.getBatchTime(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    return time2.compareTo(time1);
                } catch (Exception e) {
                    log.warn("解析批次时间失败: {} vs {}", b1.getBatchTime(), b2.getBatchTime());
                    return 0;
                }
            });

            // 存储最新新闻缓存
            String latestJson = JSONUtil.toJsonStr(latestBatches);
            stringRedisTemplate.opsForValue().set(NEWS_LATEST_KEY, latestJson, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            
            log.info("更新最新新闻缓存完成，共 {} 个批次", latestBatches.size());
        } catch (Exception e) {
            log.error("更新最新新闻缓存失败", e);
        }
    }

    /**
     * 从Redis获取新闻批次数据
     * @param batchId 批次ID
     * @return 新闻批次数据
     */
    private NewsBatch getNewsBatchFromRedis(String batchId) {
        try {
            String batchKey = NEWS_BATCH_KEY_PREFIX + batchId;
            String batchJson = stringRedisTemplate.opsForValue().get(batchKey);
            
            if (StrUtil.isBlank(batchJson)) {
                return null;
            }
            
            return JSONUtil.toBean(batchJson, NewsBatch.class);
        } catch (Exception e) {
            log.error("从Redis获取新闻批次失败: {}", batchId, e);
            return null;
        }
    }

    /**
     * 获取最新的新闻数据
     * @return 最新新闻数据列表
     */
    public List<NewsBatch> getLatestNews() {
        try {
            String latestJson = stringRedisTemplate.opsForValue().get(NEWS_LATEST_KEY);
            
            if (StrUtil.isBlank(latestJson)) {
                log.info("没有找到最新新闻缓存，尝试重新构建");
                updateLatestNewsCache();
                latestJson = stringRedisTemplate.opsForValue().get(NEWS_LATEST_KEY);
            }
            
            if (StrUtil.isBlank(latestJson)) {
                log.warn("仍然没有找到最新新闻数据");
                return new ArrayList<>();
            }
            
            List<NewsBatch> latestBatches = JSONUtil.toList(latestJson, NewsBatch.class);
            log.info("成功获取最新新闻数据，共 {} 个批次", latestBatches.size());
            
            return latestBatches;
        } catch (Exception e) {
            log.error("获取最新新闻数据失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据批次ID获取新闻数据
     * @param batchId 批次ID
     * @return 新闻批次数据
     */
    public NewsBatch getNewsByBatchId(String batchId) {
        if (StrUtil.isBlank(batchId)) {
            log.warn("批次ID为空");
            return null;
        }
        
        return getNewsBatchFromRedis(batchId);
    }

    /**
     * 获取所有批次ID列表
     * @return 批次ID列表
     */
    public Set<String> getAllBatchIds() {
        return stringRedisTemplate.opsForSet().members(NEWS_BATCHES_KEY);
    }

    /**
     * 清理过期的新闻数据
     */
    public void cleanExpiredNews() {
        try {
            Set<String> batchIds = getAllBatchIds();
            if (CollUtil.isEmpty(batchIds)) {
                return;
            }
            
            int cleanedCount = 0;
            for (String batchId : batchIds) {
                String batchKey = NEWS_BATCH_KEY_PREFIX + batchId;
                if (!stringRedisTemplate.hasKey(batchKey)) {
                    // 批次数据已过期，从批次列表中移除
                    stringRedisTemplate.opsForSet().remove(NEWS_BATCHES_KEY, batchId);
                    cleanedCount++;
                }
            }
            
            if (cleanedCount > 0) {
                log.info("清理过期新闻数据完成，共清理 {} 个批次", cleanedCount);
                // 重新更新最新新闻缓存
                updateLatestNewsCache();
            }
        } catch (Exception e) {
            log.error("清理过期新闻数据失败", e);
        }
    }
}
