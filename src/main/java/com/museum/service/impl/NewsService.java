package com.museum.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.museum.domain.dto.NewsItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 新闻服务（仅维护当前一批）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NewsService {

    private static final String KEY_ITEMS = "news:current:items"; // 整批 JSON 数组
    private static final String KEY_META = "news:current:meta";   // 元信息

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 覆盖写入当前新闻集合
     * @param items 工作流推送的新闻数组
     * @return Map 元信息（updatedAt、count）
     */
    public Map<String, Object> saveCurrent(List<NewsItemDTO> items) {
        try {
            if (CollUtil.isEmpty(items)) {
                // 空集合也允许覆盖，以便前端展示为空态
                stringRedisTemplate.opsForValue().set(KEY_ITEMS, "[]");
                Map<String, String> meta = new HashMap<>();
                meta.put("updatedAt", isoNow());
                meta.put("count", "0");
                stringRedisTemplate.opsForHash().putAll(KEY_META, meta);
                return new HashMap<>(meta);
            }
            // 计算 updatedAt：优先使用 items 中 BatchTime 最大值，否则当前时间
            String updatedAt = calcUpdatedAt(items);
            // 将数组原样 JSON 化后保存
            String json = objectMapper.writeValueAsString(items);
            log.info("NewsService.saveCurrent - 保存到Redis的JSON: {}", json);
            stringRedisTemplate.opsForValue().set(KEY_ITEMS, json);
            Map<String, String> meta = new HashMap<>();
            meta.put("updatedAt", updatedAt);
            meta.put("count", String.valueOf(items.size()));
            stringRedisTemplate.opsForHash().putAll(KEY_META, meta);
            return new HashMap<>(meta);
        } catch (Exception e) {
            log.error("保存新闻数据失败", e);
            throw new RuntimeException("保存新闻数据失败", e);
        }
    }

    /**
     * 读取当前新闻集合，按 NewsDate 降序并裁剪
     * @param limit 限制条数
     */
    public Map<String, Object> getCurrent(int limit) {
        try {
            String json = stringRedisTemplate.opsForValue().get(KEY_ITEMS);
            log.info("NewsService.getCurrent - 从Redis读取的JSON: {}", json);
            List<NewsItemDTO> items = StrUtil.isBlank(json) ? new ArrayList<>() : 
                objectMapper.readValue(json, new TypeReference<List<NewsItemDTO>>() {});
            log.info("NewsService.getCurrent - 反序列化后的items数量: {}, 内容: {}", items.size(), items);
            // 排序：按 NewsDate 降序（无法解析的放末尾）
            items = sortByNewsDateDesc(items);
            if (limit > 0 && items.size() > limit) {
                items = items.subList(0, limit);
            }
            Map<Object, Object> meta = stringRedisTemplate.opsForHash().entries(KEY_META);
            Map<String, Object> result = new HashMap<>();
            result.put("updatedAt", String.valueOf(meta.getOrDefault("updatedAt", isoNow())));
            result.put("count", Integer.parseInt(String.valueOf(meta.getOrDefault("count", String.valueOf(items.size())))));
            result.put("items", items);
            return result;
        } catch (Exception e) {
            log.error("读取新闻数据失败", e);
            throw new RuntimeException("读取新闻数据失败", e);
        }
    }

    private String calcUpdatedAt(List<NewsItemDTO> items) {
        Optional<String> maxBatchTime = items.stream()
                .map(NewsItemDTO::getBatchTime)
                .filter(StrUtil::isNotBlank)
                .max(Comparator.naturalOrder());
        return maxBatchTime.orElseGet(this::isoNow);
    }

    private String isoNow() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(ZoneId.systemDefault()));
    }

    private List<NewsItemDTO> sortByNewsDateDesc(List<NewsItemDTO> items) {
        return items.stream().sorted((a, b) -> {
            long tb = parseNewsDateToEpoch(b.getNewsDate());
            long ta = parseNewsDateToEpoch(a.getNewsDate());
            return Long.compare(tb, ta);
        }).collect(Collectors.toList());
    }

    /**
     * 解析 NewsDate（可能为日期或日期时间）为时间戳，失败返回 0
     */
    private long parseNewsDateToEpoch(String dateStr) {
        try {
            if (StrUtil.isBlank(dateStr)) return 0L;
            // 尝试多种格式
            if (dateStr.length() == 10) { // yyyy-MM-dd
                return DateUtil.parse(dateStr, "yyyy-MM-dd").getTime();
            }
            if (dateStr.length() == 19) { // yyyy-MM-dd HH:mm:ss
                return DateUtil.parse(dateStr, "yyyy-MM-dd HH:mm:ss").getTime();
            }
            // 兜底：交给 hutool 自动解析
            return DateUtil.parse(dateStr).getTime();
        } catch (Exception e) {
            return 0L;
        }
    }
}


