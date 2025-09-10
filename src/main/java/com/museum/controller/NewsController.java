package com.museum.controller;

import com.museum.config.JsonResult;
import com.museum.domain.dto.NewsItemDTO;
import com.museum.service.impl.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新闻接口（仅当前集合），独立于其他业务
 */
@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    @Resource
    private NewsService newsService;
    
    // 从配置文件读取工作流token
    @Value("${workflow.token}")
    private String workflowToken;

    /**
     * 工作流推送当前一批新闻（覆盖写入）
     * 建议在网关或拦截器增加 token 校验，这里保持最小实现
     */
    @PostMapping("/current")
    public JsonResult pushCurrent(@RequestBody List<NewsItemDTO> items,
                                  @RequestHeader(value = "X-Workflow-Token", required = false) String token) {
        try {
            // 添加调试日志
            log.info("NewsController.pushCurrent - 收到POST请求，token: {}, items数量: {}", token, items != null ? items.size() : 0);
            
            // 验证工作流token
            if (token == null || token.isEmpty()) {
                log.warn("NewsController.pushCurrent - 未提供推送令牌");
                return JsonResult.failResult("未提供推送令牌");
            }
            if (!workflowToken.equals(token)) {
                log.warn("NewsController.pushCurrent - 推送令牌无效，期望: {}, 实际: {}", workflowToken, token);
                return JsonResult.failResult("推送令牌无效");
            }
            Map<String, Object> meta = newsService.saveCurrent(items);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.putAll(meta);
            return JsonResult.result(resp);
        } catch (Exception e) {
            log.error("推送新闻失败", e);
            return JsonResult.failResult("推送新闻失败");
        }
    }

    /**
     * 获取当前新闻集合（首页展示）
     */
    @GetMapping("/current")
    public JsonResult getCurrent(@RequestParam(value = "limit", required = false, defaultValue = "12") Integer limit) {
        try {
            if (limit == null || limit <= 0) limit = 12;
            if (limit > 50) limit = 50;
            Map<String, Object> result = newsService.getCurrent(limit);
            return JsonResult.result(result);
        } catch (Exception e) {
            log.error("获取新闻失败", e);
            return JsonResult.failResult("获取新闻失败");
        }
    }
}


