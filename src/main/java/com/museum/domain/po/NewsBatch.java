package com.museum.domain.po;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 新闻批次实体类
 * </p>
 *
 * @author museum
 * @since 2025-01-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class NewsBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 批次ID
     */
    @JsonProperty("BatchId")
    private String batchId;

    /**
     * 批次时间
     */
    @JsonProperty("BatchTime")
    private String batchTime;

    /**
     * 新闻条目列表
     */
    @JsonProperty("Items")
    private List<NewsItem> items;
}
