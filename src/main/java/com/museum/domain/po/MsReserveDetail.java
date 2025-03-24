package com.museum.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ms_reserve_detail")
/**
 * 字典值实体类
 */
public class MsReserveDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private String userId;      // 用户ID
    private String userName;    // 用户名
    private Integer resId;      // 预约记录ID（展览ID）
    private String vldStat;     // 是否有效
    private String resSession;  // 场次
    
    // 冗余字段，方便展示
    private String resType;     // 预约类型
    private String resDate;     // 日期
    private String resTime;     // 时间段

    @TableField(exist = false)  // 非数据库字段，用于展示
    private String title;       // 展览标题
}
