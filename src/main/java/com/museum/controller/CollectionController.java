package com.museum.controller;


import cn.hutool.core.util.StrUtil;
import com.museum.config.JsonResult;
import com.museum.config.PageResult;
import com.museum.domain.dto.CollectionQuery;
import com.museum.domain.po.MsCollection;
import com.museum.domain.query.PageQuery;
import com.museum.service.impl.CollectionService;
import com.museum.service.impl.ReserveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @since 2023-12-19
 */
@RestController
@RequestMapping("/collect")
@RequiredArgsConstructor
@Slf4j
public class CollectionController {

    @Resource
    CollectionService collectionService;


    /**
     *用于获取页面所有藏品
     * @param pageQuery
     * @return
     */
    @PostMapping("/getdataList")
    public JsonResult getdataList(@RequestBody PageQuery pageQuery) {
        PageResult<MsCollection> users = collectionService.listMsCollectionList(pageQuery);
        return JsonResult.result(users);
    }

    /**
     * 获取单个藏品信息
     * @param pageQuery
     * @return
     */
    @PostMapping("/getdata")
    public JsonResult getdata(@RequestBody CollectionQuery pageQuery) {
        // 参数校验
        if (pageQuery == null || pageQuery.getId()==null) {
            return JsonResult.failResult("藏品ID不能为空");
        }
        try {
            MsCollection msCollection = collectionService.getMsCollection(pageQuery);
            if (msCollection == null) {
                return JsonResult.failResult("藏品不存在");
            }
            return JsonResult.result(msCollection);
        } catch (Exception e) {
            log.error("获取藏品信息失败", e);
            return JsonResult.failResult("获取藏品信息失败");
        }
    }
    @PostMapping("/getdataTop")
    public JsonResult getdataTop(@RequestBody PageQuery pageQuery) {
        String menuNm = pageQuery.getMenuName();
        if(menuNm == null) {
            pageQuery.setMenuName("热门藏品");
            menuNm = "热门藏品";
        }
        PageResult pageResult = null;
        pageResult = collectionService.listMsCollectionTop(pageQuery);
        return JsonResult.result(pageResult);
    }


}
