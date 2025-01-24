package com.museum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.config.ExcepUtil;
import com.museum.config.PageResult;
import com.museum.damain.dto.ReserveQuery;
import com.museum.damain.po.MsCollection;
import com.museum.damain.po.MsReserve;
import com.museum.damain.po.MsReserveDetial;
import com.museum.mapper.CollectionMapper;
import com.museum.mapper.ReserveDetialMapper;
import com.museum.mapper.ReserveMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 * @since 2023-22-29
 */
@Service
public class ReserveDetailService extends ServiceImpl<ReserveDetialMapper, MsReserveDetial> implements IService<MsReserveDetial> {

    @Resource
    ReserveMapper reserveMapper;
    @Resource
    CollectionMapper collectionMapper;

    /**
     * 获取用户预约列表
     * @return
     */
    public PageResult<MsReserveDetial> listMsReserveDetail(ReserveQuery pageQuery) {
        LambdaQueryChainWrapper<MsReserveDetial> lambdaQueryChainWrapper = lambdaQuery();
        if(null != pageQuery.getCateId()) {
            lambdaQueryChainWrapper.eq(MsReserveDetial::getCateId, pageQuery.getCateId());
        }
        if(null != pageQuery.getUserId()) {
            lambdaQueryChainWrapper.eq(MsReserveDetial::getUserId, pageQuery.getUserId());
        }
        Page<MsReserveDetial> page = lambdaQueryChainWrapper.page(pageQuery.toMpPage());
        return PageResult.of(page, page.getRecords());
    }

    /**
     * 编辑预约详情
     * @param detial
     */
    public void editDetail(MsReserveDetial detial) throws Exception {
        QueryWrapper<MsReserveDetial> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MsReserveDetial::getUserId, detial.getUserId()).eq(MsReserveDetial::getResId, detial.getResId())
                .eq(MsReserveDetial::getVldStat,"1").eq(MsReserveDetial::getCateId, detial.getCateId());
        List<MsReserveDetial> msReserveDetials = baseMapper.selectList(queryWrapper);
        if(!msReserveDetials.isEmpty() && "1".equals(detial.getVldStat())) {
            ExcepUtil.throwErr("已存在有效的预约申请，无法修改");
        }
        saveOrUpdate(detial);
        updateMsReserveResdSum(detial);
    }
    /**
     * 添加预约详情
     *   private Integer id;
     *     private String userId; // 用户ID
     *     private String userName; // 用户名
     *     private String resId; // 预约记录ID
     *     private String cateId; // 展品ID
     *     private String cateTitle; // 展品名称
     *     private String resType; // 预约类型
     *     private String resDate; // 日期
     *     private String resTime; // 时间段
     *     private String vldStat; // 是否有效
     *     private String resSession; //场次
     * @param detial
     */
    public void addDetail(MsReserveDetial detial) throws Exception {
        QueryWrapper<MsReserveDetial> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MsReserveDetial::getUserId, detial.getUserId()).eq(MsReserveDetial::getResId, detial.getResId())
                .eq(MsReserveDetial::getVldStat,"1").eq(MsReserveDetial::getCateId, detial.getCateId());
        List<MsReserveDetial> msReserveDetials = baseMapper.selectList(queryWrapper);
        if(!msReserveDetials.isEmpty()) {
            ExcepUtil.throwErr("已经预约过，不允许重复预约");
        }
        MsReserve msReserve = reserveMapper.selectById(detial.getResId());
        if(null != msReserve) {
            detial.setResType(msReserve.getResTyp());
            detial.setResDate(msReserve.getResDate());
            detial.setResTime(msReserve.getResTime());
            detial.setResSession(msReserve.getResSession());
            detial.setVldStat("1"); // 有效

            MsCollection collection = collectionMapper.selectById(msReserve.getCateId());
            detial.setCateTitle(collection.getTitle());
            detial.setCateId(msReserve.getCateId());
        }else {
            ExcepUtil.throwErr("原始记录已不存在，不允许修改！");
        }
        save(detial);
        updateMsReserveResdSum(detial);
    }

    /**
     * 更新实际预约人数
     */
    public void updateMsReserveResdSum(MsReserveDetial detial) {
        // 更新实际预约人数
        MsReserve msReserve = reserveMapper.selectById(detial.getResId());
        QueryWrapper<MsReserveDetial> detialQueryWrapper = new QueryWrapper<>();
        detialQueryWrapper.lambda().eq(MsReserveDetial::getResId, detial.getResId()).eq(MsReserveDetial::getVldStat, "1");
        List<MsReserveDetial> reserveDetials = baseMapper.selectList(detialQueryWrapper);
        msReserve.setResdSum(reserveDetials.size());
        reserveMapper.updateById(msReserve);
    }

    /**
     * 删除预约详情
     * @param id
     * @throws Exception
     */
    public void delDetail(Integer id) throws Exception {
        MsReserveDetial detial = baseMapper.selectById(id);
        if(detial == null) {
            throw new Exception("找不到原始记录，删除失败！");
        }
        removeById(id);
        updateMsReserveResdSum(detial);
    }
}
