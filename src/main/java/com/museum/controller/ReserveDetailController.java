package com.museum.controller;


import com.museum.config.JsonResult;
import com.museum.config.PageResult;
import com.museum.domain.dto.ReserveQuery;
import com.museum.domain.po.MsReserveDetail;
import com.museum.service.impl.ReserveDetailService;
import com.museum.service.impl.ReserveOrderAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import com.museum.domain.po.MsCollection;
import com.museum.domain.po.MsReserve;
import com.museum.domain.dto.MsUserDTO;
import com.museum.mapper.CollectionMapper;
import com.museum.mapper.ReserveCollectionMapper;
import com.museum.service.impl.ReserveService;
import com.museum.utils.UserHolder;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @since 2023-12-19
 */
@RestController
@RequestMapping("/reserveDetail")
@RequiredArgsConstructor
public class ReserveDetailController {

    @Resource
    ReserveDetailService reserveDetailService;

    @Resource
    private ReserveService reserveService;

    @Resource
    private ReserveCollectionMapper reserveCollectionMapper;

    @Resource
    private CollectionMapper collectionMapper;

    @Resource
    private ReserveOrderAsyncService reserveOrderAsyncService;

    @PostMapping("/listDetailReserve")
    public JsonResult listDetailReserve(@RequestBody ReserveQuery pageQuery) {
        PageResult<MsReserveDetail> result = reserveDetailService.listMsReserveDetail(pageQuery);
        return JsonResult.result(result);
    }

    @PostMapping("/addDetail")
    public JsonResult addDetail(@RequestBody MsReserveDetail detail) {
        try {
            return reserveDetailService.addDetail(detail);
           // return JsonResult.result("成功！");
        }catch (Exception e){
            e.printStackTrace();
            return JsonResult.failResult(e.getMessage());
        }
    }

    @PostMapping("/delDetail")
    public JsonResult delDetail(@RequestBody Map<String, Long> request) throws Exception {
        Long resDetailId = request.get("id");
        return (reserveDetailService.delDetail(resDetailId) ? 
                JsonResult.result("删除成功") : 
                JsonResult.failResult("网络问题！删除异常"));
    }

    /**
     * 简化的预约接口，只需要展览ID和用户Token，用于并发测试
     * @param reserveId 展览ID
     * @return 预约结果
     */
    @PostMapping("/addDetailQuick/{reserveId}")
    public JsonResult addDetailQuick(@PathVariable("reserveId") Integer reserveId) {
        try {
            // 从ThreadLocal中获取当前登录用户
            MsUserDTO userDTO = UserHolder.getUser();
            if (userDTO == null) {
                return JsonResult.failResult("用户未登录");
            }
            
            // 获取展览信息
            MsReserve reserve = reserveService.getById(reserveId);
            if (reserve == null) {
                return JsonResult.failResult("展览不存在");
            }
            
            // 获取展览关联的藏品ID
            List<Integer> cateIds = reserveCollectionMapper.findCateIdsByReserveId(reserveId);
            if (cateIds.isEmpty()) {
                return JsonResult.failResult("展览未关联藏品");
            }
            
            // 查询第一个关联藏品的信息作为预约的关联藏品
            MsCollection collection = collectionMapper.selectById(cateIds.get(0));
            if (collection == null) {
                return JsonResult.failResult("关联藏品不存在");
            }
            
            // 构建预约详情对象
            MsReserveDetail detail = new MsReserveDetail();
            detail.setUserId(userDTO.getId().toString());
            detail.setUserName(userDTO.getUsername());
            detail.setResId(reserveId);
            detail.setResType(reserve.getResTyp());
            detail.setResDate(reserve.getResDate());
            detail.setResTime(reserve.getResTime());
            detail.setResSession(reserve.getResSession());
            
//            // 调用现有的添加预约方法
//            return reserveDetailService.addDetail(detail);
            //调用异步添加预约的方法
            return reserveOrderAsyncService.addDetailAsync(detail);
        } catch (Exception e) {
            e.printStackTrace();
            return JsonResult.failResult(e.getMessage());
        }
    }

}
