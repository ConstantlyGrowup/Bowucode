package com.museum.controller;


import com.museum.config.JsonResult;
import com.museum.config.PageResult;
import com.museum.damain.dto.ReserveQuery;
import com.museum.damain.po.MsCollection;
import com.museum.damain.po.MsReserve;
import com.museum.damain.query.PageQuery;
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

    @PostMapping("/listMsReserve")
    public JsonResult listMsReserve(@RequestBody ReserveQuery pageQuery) {
        PageResult<MsReserve> result = reserveService.listMsReserve(pageQuery);
        return JsonResult.result(result);
    }

    @PostMapping("/listMsReserveClient")
    public JsonResult listMsReserveClient(@RequestBody ReserveQuery pageQuery) {
        PageResult<MsReserve> result = reserveService.listMsReserveClient(pageQuery);
        return JsonResult.result(result);
    }

    @PostMapping("/addMsReserve")
    public JsonResult addMsReserve(@RequestBody MsReserve msReserve) {
        try {
            reserveService.addMsReserve(msReserve);
            return JsonResult.result("成功！");
        }catch (Exception e){
            e.printStackTrace();
            return JsonResult.failResult(e.getMessage());
        }
    }

//    @PostMapping("/addMsReserve")
//    public JsonResult addMsReserve(@RequestBody MsReserve msReserve) {
//        try {
//            // 直接访问MsReserve对象的collectionId属性
//            Integer[] collectionId = msReserve.getCateIds(); // 假设getCollectionId是自动生成的getter方法
//
//            // 检查预约信息中是否包含藏品ID
//            if (collectionId == null||collectionId.length==0) {
//                return JsonResult.failResult("预约信息中缺少藏品ID，请提供藏品ID！");
//            }
//
////            // 检查该藏品ID是否存在
////            MsCollection collection = collectionService.getById(collectionId);
////            if (collection == null) {
////                return JsonResult.failResult("指定的藏品ID不存在，请检查后重试！");
////            }
//
//            // 如果藏品存在，继续添加预约信息
//            reserveService.addMsReserve(msReserve);
//            return JsonResult.result("成功！");
//        } catch (Exception e) {
//            e.printStackTrace();
//            return JsonResult.failResult(e.getMessage());
//        }
//    }


    @PostMapping("/editMsReserve")
    public JsonResult editMsReserve(@RequestBody MsReserve msReserve) {
        try {
            reserveService.editMsReserve(msReserve);
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }

    @PostMapping("/delMsReserve")
    public JsonResult delMsReserve(@RequestBody MsReserve msReserve) {
        try {
            reserveService.delMsReserve(msReserve.getId());
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }
}
