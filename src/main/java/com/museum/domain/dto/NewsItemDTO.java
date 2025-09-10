package com.museum.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 新闻条目 DTO（与工作流推送结构保持一致）
 * 保留字段命名：NewsDate/Content/Media/Link/Title/BatchId/BatchTime
 */
@Data
public class NewsItemDTO {
    // 新闻时间，可能为日期或日期时间，例如：2025-09-10 或 2025-09-09 17:00:00
    @JsonProperty("NewsDate")
    private String NewsDate;
    // 新闻摘要/正文精简
    @JsonProperty("Content")
    private String Content;
    // 媒体来源
    @JsonProperty("Media")
    private String Media;
    // 原文链接
    @JsonProperty("Link")
    private String Link;
    // 标题
    @JsonProperty("Title")
    private String Title;
    // 批次标识（虽然我们不保留历史批次，但工作流会传入，便于记录）
    @JsonProperty("BatchId")
    private String BatchId;
    // 批次时间
    @JsonProperty("BatchTime")
    private String BatchTime;
}


