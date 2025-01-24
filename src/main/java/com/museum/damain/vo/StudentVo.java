package com.museum.damain.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @since 2023-12-19
 */
@Data
@ApiModel(description = "学生信息")
public class StudentVo {

    @ApiModelProperty("学生id")
    private Integer id;
    @ApiModelProperty("学生名称")
    private String name;
    @ApiModelProperty("学生年龄")
    private Integer age;
    @ApiModelProperty("学生班级")
    private Integer classId;
    @ApiModelProperty("学生成绩")
    private Integer score;


}
