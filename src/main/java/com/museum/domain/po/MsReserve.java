package com.museum.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ms_reserve")
/**
 * 字典值实体类
 */
public class MsReserve implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String title;
    private String resTyp;
    private Integer cateId;
    private String cateName;
    private Integer resSum;
    private String resDate;
    private String resTime;
    private String resSession;
    private String resDes;
    private Integer resdSum;
    private String crtTm;
    @TableField(exist = false)
    private Integer[] cateIds;
}
