package com.museum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.config.ExcepUtil;
import com.museum.config.PageResult;
import com.museum.damain.dto.ReserveQuery;
import com.museum.damain.po.FeedBack;
import com.museum.damain.po.MsCollection;
import com.museum.damain.po.MsReserve;
import com.museum.damain.po.MsReserveDetial;
import com.museum.damain.query.PageQuery;
import com.museum.mapper.CollectionMapper;
import com.museum.mapper.ReserveDetialMapper;
import com.museum.mapper.ReserveMapper;
import com.museum.utils.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 * @since 2023-22-29
 */
@Service
public class ReserveService extends ServiceImpl<ReserveMapper, MsReserve> implements IService<MsReserve> {
    @Resource
    private ReserveDetialMapper reserveDetialMapper;
    @Resource
    CollectionMapper collectionMapper;
    /**
     * 获取展品列表
     * @return
     */
    public PageResult<MsReserve> listMsReserve(ReserveQuery pageQuery) {
        LambdaQueryChainWrapper<MsReserve> lambdaQueryChainWrapper = lambdaQuery().like(MsReserve::getTitle, pageQuery.getName());
        if(null != pageQuery.getCateId()) {
            lambdaQueryChainWrapper.eq(MsReserve::getCateId, pageQuery.getCateId());
        }
        Page<MsReserve> page = lambdaQueryChainWrapper.like(MsReserve::getTitle, pageQuery.getName()).page(pageQuery.toMpPage());
        return PageResult.of(page, page.getRecords());
    }

    /**
     * 客户端过滤过时的预约，并显示预约
     * @param pageQuery
     * @return
     */
    public PageResult<MsReserve> listMsReserveClient(ReserveQuery pageQuery) {
        LocalDate today = LocalDate.now();
        String todayString = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

        QueryWrapper<MsReserve> queryWrapper = new QueryWrapper<>();

        // 添加过滤条件，过滤掉已经过时的预约信息
        queryWrapper.lambda()
                .eq(pageQuery.getCateId() != null, MsReserve::getCateId, pageQuery.getCateId())
                .ge(MsReserve::getResDate, todayString);

        Page<MsReserve> page = this.page(pageQuery.toMpPage(), queryWrapper);
        return PageResult.of(page, page.getRecords());
    }

    public PageResult<MsReserve> listTop(PageQuery pageQuery) {
        if("近期解说".equals(pageQuery.getMenuName())) {
            Page<MsReserve> page = lambdaQuery().orderByDesc(MsReserve::getCrtTm).page(pageQuery.toMpPage());
            return PageResult.of(page, page.getRecords());
        } else if("热门解说".equals(pageQuery.getMenuName())) {
            Page<MsReserve> page = lambdaQuery().orderByDesc(MsReserve::getResdSum).page(pageQuery.toMpPage());
            return PageResult.of(page, page.getRecords());
        }
        return null;
    }

    /**
     * 编辑预约信息
     * @param msReserve
     */
    public void editMsReserve(MsReserve msReserve) {
        msReserve.setCrtTm(StringUtils.getNowDateTIme());
        saveOrUpdate(msReserve);
    }

//    /**
//     * 添加预约信息
//     * @param msReserve
//     */
//    public void addMsReserve(MsReserve msReserve) {
//        Integer[] cateIds = msReserve.getCateIds();
//        for (Integer cateId:cateIds) {
//            msReserve.setCateId(cateId);
//            MsCollection msCollection =  collectionMapper.selectById(cateId);
//            if(null != msCollection) {
//                msReserve.setCateName(msCollection.getTitle());
//            }
//            msReserve.setResdSum(0);
//            msReserve.setCrtTm(StringUtils.getNowDateTIme());
//            save(msReserve);
//        }
//    }


    /**
     * 添加预约信息
     * @param msReserve
     */

    public void addMsReserve(MsReserve msReserve) {
        // 获取藏品ID数组
        Integer[] cateIds = msReserve.getCateIds();
        if (cateIds == null || cateIds.length == 0) {
            throw new IllegalArgumentException("预约信息中缺少藏品ID，请提供藏品ID！");
        }

        // 获取所有相关藏品的信息
        List<MsCollection> collections = new ArrayList<>();
        for (Integer cateId : cateIds) {
            MsCollection msCollection = collectionMapper.selectById(cateId);
            if (msCollection != null) {
                collections.add(msCollection);
            } else {
                throw new IllegalArgumentException("指定的藏品ID不存在，请检查后重试！");
            }
        }

//        // 设置预约信息中的藏品名称
//        StringBuilder cateNames = new StringBuilder();
//        for (MsCollection collection : collections) {
//            if (cateNames.length() > 0) {
//                cateNames.append(", ");
//            }
//            cateNames.append(collection.getTitle());
//        }
//        msReserve.setCateName(cateNames.toString());
//
//        // 设置预约信息的其他属性
//        msReserve.setResdSum(0);
//        msReserve.setCrtTm(StringUtils.getNowDateTIme());
//
//        // 保存预约信息
//        save(msReserve);
        for (Integer cateId:cateIds) {
            msReserve.setCateId(cateId);
            MsCollection msCollection =  collectionMapper.selectById(cateId);
            if(null != msCollection) {
                msReserve.setCateName(msCollection.getTitle());
            }
            msReserve.setResdSum(0);
            msReserve.setCrtTm(StringUtils.getNowDateTIme());
            save(msReserve);
        }
    }

    /**
     * 删除预约信息
     * @param id
     * @throws Exception
     */
    public void delMsReserve(Integer id) throws Exception {
        MsReserve msReserve = baseMapper.selectById(id);
        if(msReserve == null) {
            throw new Exception("找不到原始记录，删除失败！");
        }
        QueryWrapper<MsReserveDetial> detialQueryWrapper = new QueryWrapper<>();
        detialQueryWrapper.lambda().eq(MsReserveDetial::getResId, id).eq(MsReserveDetial::getVldStat,"1");
        List<MsReserveDetial> data = reserveDetialMapper.selectList(detialQueryWrapper);
        if(!data.isEmpty()) {
            ExcepUtil.throwErr("该预约下存在有效的预约记录，无法删除！");
        }
        removeById(id);
    }

    public List<MsReserveDetial> getUserReserve(Integer userId, Integer resId) {
        QueryWrapper<MsReserveDetial> detialQueryWrapper = new QueryWrapper<>();
        if(null != userId) {
            detialQueryWrapper.lambda().eq(MsReserveDetial::getUserId, userId);
        }
        if(null != resId) {
            detialQueryWrapper.lambda().eq(MsReserveDetial::getResId, resId);
        }
        List<MsReserveDetial> detials = reserveDetialMapper.selectList(detialQueryWrapper);
        return detials;
    }
}
