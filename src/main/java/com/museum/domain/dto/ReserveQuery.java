package com.museum.domain.dto;

import com.museum.domain.query.PageQuery;
import lombok.Data;

@Data
public class ReserveQuery extends PageQuery {
    private Integer userId;
    private Integer resId;
    private Integer cateId;
    private String userName;
}
