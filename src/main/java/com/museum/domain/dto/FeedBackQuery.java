package com.museum.domain.dto;

import com.museum.domain.query.PageQuery;
import lombok.Data;

@Data
public class FeedBackQuery extends PageQuery {
    private Integer userId;
    private String userName;
    private String cateId;
}
