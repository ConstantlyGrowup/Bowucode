package com.museum.domain.po;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 新闻条目实体类
 * </p>
 *
 * @author museum
 * @since 2025-01-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class NewsItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 新闻条目ID
     */
    @JsonProperty("ItemId")
    private String itemId;

    /**
     * 新闻标题
     */
    @JsonProperty("Title")
    private String title;

    /**
     * 新闻内容摘要
     */
    @JsonProperty("Content")
    private String content;

    /**
     * 新闻发布时间
     */
    @JsonProperty("NewsDate")
    private String newsDate;

    /**
     * 媒体来源
     */
    @JsonProperty("Media")
    private String media;

    /**
     * 新闻链接
     */
    @JsonProperty("Link")
    private String link;
}
