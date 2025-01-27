package com.museum.domain.query;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.constants.Constant;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@ApiModel(description = "分页请求参数")
@Accessors(chain = true)
public class PageQuery {
    public static final Integer DEFAULT_PAGE_SIZE = 20;
    public static final Integer DEFAULT_PAGE_NUM = 1;

    private Integer pagenum = DEFAULT_PAGE_NUM;

    private Integer pagesize = DEFAULT_PAGE_SIZE;

    private String name;
    private Integer id;
    private String menuName;

    public String getName() {
        if(name == null || "null".equalsIgnoreCase(name)) {
            return "";
        }
        return name;
    }

    public int from(){
        return (pagenum - 1) * pagesize;
    }

    public <T> Page<T> toMpPage(OrderItem ... orderItems) {
        Page<T> page = new Page<>(pagenum, pagesize);
        // 是否手动指定排序方式
        if (orderItems != null && orderItems.length > 0) {
            for (OrderItem orderItem : orderItems) {
                page.addOrder(orderItem);
            }
            return page;
        }
        return page;
    }

    public <T> Page<T> toMpPage(String defaultSortBy, boolean isAsc) {
        Page<T> page = new Page<>(pagenum, pagesize);
        OrderItem orderItem = new OrderItem();
        page.addOrder(orderItem);
        return page;
    }
    public <T> Page<T> toMpPageDefaultSortByCreateTimeDesc() {
        return toMpPage(Constant.DATA_FIELD_NAME_CREATE_TIME, false);
    }
}
