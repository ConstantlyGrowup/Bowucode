package com.museum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.config.ExcepUtil;
import com.museum.config.JsonResult;
import com.museum.config.PageResult;
import com.museum.domain.dto.ReserveQuery;
import com.museum.domain.po.MsReserve;
import com.museum.domain.po.MsReserveDetail;
import com.museum.mapper.CollectionMapper;
import com.museum.mapper.ReserveCollectionMapper;
import com.museum.mapper.ReserveDetialMapper;
import com.museum.mapper.ReserveMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 * @since 2023-22-29
 */
@Service
public class ReserveDetailService extends ServiceImpl<ReserveDetialMapper, MsReserveDetail> implements IService<MsReserveDetail> {

    @Resource
    ReserveMapper reserveMapper;
    @Resource
    CollectionMapper collectionMapper;
    @Resource
    private ReserveCollectionMapper reserveCollectionMapper;

    /**
     * 获取用户预约列表
     * @return
     */
    public PageResult<MsReserveDetail> listMsReserveDetail(ReserveQuery pageQuery) {
        LambdaQueryChainWrapper<MsReserveDetail> lambdaQueryChainWrapper = lambdaQuery();
        if(null != pageQuery.getUserId()) {
            lambdaQueryChainWrapper.eq(MsReserveDetail::getUserId, pageQuery.getUserId());
        }
        
        Page<MsReserveDetail> page = lambdaQueryChainWrapper.page(pageQuery.toMpPage());
        List<MsReserveDetail> records = page.getRecords();
        
        // 获取每条预约记录对应的展览信息
        for (MsReserveDetail detail : records) {
            MsReserve reserve = reserveMapper.selectById(detail.getResId());
            if (reserve != null) {
                detail.setTitle(reserve.getTitle());  // 使用展览的标题
                detail.setResType(reserve.getResTyp());
            }
        }
        
        return PageResult.of(page, records);
    }

    /**
     * 编辑预约详情
     * @param detail 预约详情
     */
    public void editDetail(MsReserveDetail detail) throws Exception {
        // 检查是否存在有效的预约记录
        QueryWrapper<MsReserveDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(MsReserveDetail::getUserId, detail.getUserId())
                .eq(MsReserveDetail::getResId, detail.getResId())
                .eq(MsReserveDetail::getVldStat, "1");
        
        List<MsReserveDetail> msReserveDetails = baseMapper.selectList(queryWrapper);
        
        // 如果找到有效预约记录，且当前操作是要设置为有效，则抛出异常
        if (!msReserveDetails.isEmpty() && "1".equals(detail.getVldStat())) {
            ExcepUtil.throwErr("已存在有效的预约申请，无法修改");
        }
        
        // 更新预约详情
        saveOrUpdate(detail);
        
        // 更新展览的预约人数
        updateReserveResdSum(detail.getResId());
    }

    /**
     * 添加预约详情
     */
    public JsonResult addDetail(MsReserveDetail detail) throws Exception {

        //1.检查日期，预约是否过期
        //当天日期
        LocalDate now=LocalDate.now();
        //日期转换
        DateTimeFormatter formatter=DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate resDate = LocalDate.parse(detail.getResDate(), formatter);
        //如果已经过期
        if(resDate.isBefore(now))
        {
           return JsonResult.failResult("预约已经过期！");
        }
        //2.检查库存余量是否充足
        MsReserve reserve = reserveMapper.selectById(detail.getResId());//得到单个展览
        Integer currentSum = reserve.getResdSum();
        if(currentSum+1>reserve.getResSum())//如果加上当前预约数大于预约总数
        {
            return JsonResult.failResult("预约超额！");
        }
        //3.检查是否先前有过预约
        QueryWrapper<MsReserveDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(MsReserveDetail::getUserId, detail.getUserId())
                .eq(MsReserveDetail::getResId, detail.getResId())
                .eq(MsReserveDetail::getVldStat, "1");

        if (count(queryWrapper) > 0) {
            return JsonResult.failResult("已经预约过该展览，不能重复预约");
        }
        //4.条件均满足，预约人数+1
        detail.setVldStat("1");
        reserve.setResdSum(currentSum+1);
        save(detail);
        reserveMapper.updateById(reserve);
        return JsonResult.result(detail.getId());
    }



    /**
     * 删除预约详情
     * @param id
     * @throws Exception
     */
    public void delDetail(Integer id) throws Exception {
        MsReserveDetail detial = baseMapper.selectById(id);
        if(detial == null) {
            throw new Exception("找不到原始记录，删除失败！");
        }
        removeById(id);
        updateReserveResdSum(detial.getResId());
    }

    /**
     * 更新展览预约人数
     */
    private void updateReserveResdSum(Integer reserveId) {
        QueryWrapper<MsReserveDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(MsReserveDetail::getResId, reserveId)
                .eq(MsReserveDetail::getVldStat, "1");
        
        int count = count(queryWrapper);
        
        MsReserve reserve = reserveMapper.selectById(reserveId);
        if (reserve != null) {
            reserve.setResdSum(count);
            reserveMapper.updateById(reserve);
        }
    }

    /**
     * 取消预约
     */
    public void cancelDetail(Integer detailId) throws Exception {
        MsReserveDetail detail = getById(detailId);
        if (detail == null) {
            throw new IllegalArgumentException("预约记录不存在");
        }
        
        detail.setVldStat("0");
        updateById(detail);
        
        updateReserveResdSum(detail.getResId());
    }
}
