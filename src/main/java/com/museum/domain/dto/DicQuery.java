package com.museum.domain.dto;

import com.museum.domain.query.PageQuery;
import lombok.Data;

@Data
public class DicQuery extends PageQuery {
    private String dicTyp;
    private String dicDesc;
    private String dicValue;
}
