package com.museum.controller;


import com.museum.config.JsonResult;
import com.museum.config.PageResult;
import com.museum.domain.dto.ReserveQuery;
import com.museum.domain.po.MsReserve;
import com.museum.service.impl.CollectionService;
import com.museum.service.impl.ReserveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @since 2023-12-19
 */
@RestController
@RequestMapping("/reserve")
@RequiredArgsConstructor
public class ReserveController {

    @Resource
    ReserveService reserveService;

    @Resource
    CollectionService collectionService;


    @PostMapping("/listMsReserveClient")
    public JsonResult listMsReserveClient(@RequestBody ReserveQuery pageQuery) {
        PageResult<MsReserve> result = reserveService.listMsReserveClient(pageQuery);
        return JsonResult.result(result);
    }
}
