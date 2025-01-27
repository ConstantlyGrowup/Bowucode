package com.museum.domain.dto;

import com.museum.domain.query.PageQuery;
import lombok.Data;

@Data
public class CollectionQuery extends PageQuery {
    private String cateId;
}
