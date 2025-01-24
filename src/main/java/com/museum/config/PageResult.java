package com.museum.config;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.utils.BeanUtils;
import com.museum.utils.CollUtils;
import com.museum.utils.Convert;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "分页结果")
public class PageResult<T> {
    @ApiModelProperty("总条数")
    protected Long total;
    @ApiModelProperty("总页码数")
    protected Long pages;
    @ApiModelProperty("当前页数据")
    protected List<T> list;

    public static <T> PageResult<T> empty(Long total, Long pages) {
        return new PageResult<>(total, pages, CollUtils.emptyList());
    }
    public static <T> PageResult<T> empty(Page<?> page) {
        return new PageResult<>(page.getTotal(), page.getPages(), CollUtils.emptyList());
    }

    public static <T> PageResult<T> of(Page<T> page) {
        if(page == null){
            return new PageResult<>();
        }
        if (CollUtils.isEmpty(page.getRecords())) {
            return empty(page);
        }
        return new PageResult<>(page.getTotal(), page.getPages(), page.getRecords());
    }
    public static <T,R> PageResult<T> of(Page<R> page, Function<R, T> mapper) {
        if(page == null){
            return new PageResult<>();
        }
        if (CollUtils.isEmpty(page.getRecords())) {
            return empty(page);
        }
        return new PageResult<>(page.getTotal(), page.getPages(),
                page.getRecords().stream().map(mapper).collect(Collectors.toList()));
    }
    public static <T> PageResult<T> of(Page<?> page, List<T> list) {
        return new PageResult<>(page.getTotal(), page.getPages(), list);
    }

    public static <T, R> PageResult<T> of(Page<R> page, Class<T> clazz) {
        return new PageResult<>(page.getTotal(), page.getPages(), BeanUtils.copyList(page.getRecords(), clazz));
    }

    public static <T, R> PageResult<T> of(Page<R> page, Class<T> clazz, Convert<R, T> convert) {
        return new PageResult<>(page.getTotal(), page.getPages(), BeanUtils.copyList(page.getRecords(), clazz, convert));
    }

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    public boolean isEmpty(){
        return list == null || list.size() == 0;
    }
}
