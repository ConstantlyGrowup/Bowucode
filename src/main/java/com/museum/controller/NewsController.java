package com.museum.controller;

import com.museum.config.JsonResult;
import com.museum.domain.po.NewsBatch;
import com.museum.service.impl.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 新闻控制器
 * </p>
 *
 * @author museum
 * @since 2025-01-11
 */
@Slf4j
@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    @Resource
    private NewsService newsService;

    /**
     * 接收工作流推送的新闻数据
     * @param newsBatches 新闻批次数据
     * @return 处理结果
     */
    @PostMapping("/push")
    public JsonResult pushNews(@RequestBody List<NewsBatch> newsBatches) {
        try {
            log.info("接收到新闻推送请求，批次数量: {}", newsBatches.size());
            
            // 处理新闻数据并存储到Redis
            newsService.processAndStoreNews(newsBatches);
            
            log.info("新闻数据推送处理完成");
            return JsonResult.result("新闻数据推送成功");
        } catch (Exception e) {
            log.error("新闻数据推送处理失败", e);
            return JsonResult.failResult("新闻数据推送失败: " + e.getMessage());
        }
    }

    /**
     * 获取最新的新闻数据（供客户端展示）
     * @return 新闻数据列表
     */
    @PostMapping("/list")
    public JsonResult getLatestNews() {
        try {
            log.info("客户端请求获取最新新闻数据");
            
            // 从Redis获取最新的新闻数据
            List<NewsBatch> latestNews = newsService.getLatestNews();
            
            log.info("成功返回新闻数据，批次数量: {}", latestNews.size());
            return JsonResult.result(latestNews);
        } catch (Exception e) {
            log.error("获取新闻数据失败", e);
            return JsonResult.failResult("获取新闻数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定批次的新闻数据
     * @param batchId 批次ID
     * @return 新闻数据
     */
    @PostMapping("/getByBatchId")
    public JsonResult getNewsByBatchId(@RequestParam String batchId) {
        try {
            log.info("客户端请求获取指定批次新闻数据，批次ID: {}", batchId);
            
            // 从Redis获取指定批次的新闻数据
            NewsBatch newsBatch = newsService.getNewsByBatchId(batchId);
            
            if (newsBatch == null) {
                return JsonResult.failResult("未找到指定批次的新闻数据");
            }
            
            log.info("成功返回指定批次新闻数据");
            return JsonResult.result(newsBatch);
        } catch (Exception e) {
            log.error("获取指定批次新闻数据失败", e);
            return JsonResult.failResult("获取指定批次新闻数据失败: " + e.getMessage());
        }
    }
}
