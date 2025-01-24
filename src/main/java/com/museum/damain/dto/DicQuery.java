package com.museum.damain.dto;

import com.museum.damain.query.PageQuery;
import lombok.Data;

@Data
public class DicQuery extends PageQuery {
    private String dicTyp;
    private String dicDesc;
    private String dicValue;
}
