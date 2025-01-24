package com.museum.damain.dto;

import com.museum.damain.query.PageQuery;
import lombok.Data;

@Data
public class ReserveQuery extends PageQuery {
    private Integer userId;
    private Integer resId;
    private Integer cateId;
    private String userName;
}
