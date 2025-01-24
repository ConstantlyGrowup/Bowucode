package com.museum.damain.dto;

import com.museum.damain.query.PageQuery;
import lombok.Data;

@Data
public class FeedBackQuery extends PageQuery {
    private Integer userId;
    private String userName;
    private String cateId;
}
