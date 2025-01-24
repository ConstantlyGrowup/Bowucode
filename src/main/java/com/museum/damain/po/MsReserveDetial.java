package com.museum.damain.po;

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
@TableName("ms_reserve_detial")
/**
 * 字典值实体类
 */
public class MsReserveDetial implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String userId; // 用户ID
    private String userName; // 用户名
    private String resId; // 预约记录ID
    private Integer cateId; // 展品ID
    private String cateTitle; // 展品名称
    private String resType; // 预约类型
    private String resDate; // 日期
    private String resTime; // 时间段
    private String vldStat; // 是否有效
    private String resSession; //场次
}
