package com.museum.damain.dto;

import com.museum.damain.query.PageQuery;
import lombok.Data;

@Data
public class CollectionQuery extends PageQuery {
    private String cateId;
}
