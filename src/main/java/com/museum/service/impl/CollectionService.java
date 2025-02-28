package com.museum.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.config.PageResult;
import com.museum.domain.dto.CollectionQuery;
import com.museum.domain.po.MsCollection;
import com.museum.domain.query.PageQuery;
import com.museum.mapper.CollectionMapper;
import com.museum.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.museum.constants.Constant.CACHE_CATE_TTL;
import static com.museum.constants.Constant.CATE_LIST;

/**
 * <p>
 *  服务实现类
 * </p>
 * @since 2023-22-29
 */
@Service
public class CollectionService extends ServiceImpl<CollectionMapper, MsCollection> implements IService<MsCollection> {
    //TODO:更新数据库数据的操作，需要保持数据一致性
    //TODO:缓存藏品信息，需结合布隆过滤器、逻辑过期、缓存空值去预防缓存击穿、缓存穿透
    //TODO:先做一个细分的整体商户缓存

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取coll-list该页面下所有藏品
     * @param pageQuery
     * @return
     */
    public PageResult<MsCollection> listMsCollectionList(PageQuery pageQuery)
    {
        //按照分页查询
        Page<MsCollection> page = lambdaQuery().orderByDesc(MsCollection::getCrtTm).page(pageQuery.toMpPage());
        return PageResult.of(page,page.getRecords());
    }
    public PageResult<MsCollection> listMsCollection(CollectionQuery pageQuery) {
        LambdaQueryChainWrapper<MsCollection> lambdaQueryChainWrapper = lambdaQuery().like(MsCollection::getTitle, pageQuery.getName());
        if(null != pageQuery.getId()) {
            lambdaQueryChainWrapper.eq(MsCollection::getId, pageQuery.getId());
            MsCollection msCollection = getById(pageQuery.getId());
            if(msCollection.getViewCnt() == null) {
                msCollection.setViewCnt(1);
            }else {
                msCollection.setViewCnt(msCollection.getViewCnt() + 1);
            }
            updateById(msCollection);
        }
        if(null != pageQuery.getCateId()) {
            lambdaQueryChainWrapper.eq(MsCollection::getCateId, pageQuery.getCateId());
        }
        Page<MsCollection> page = lambdaQueryChainWrapper.page(pageQuery.toMpPage());
        return PageResult.of(page, page.getRecords());
    }
    /**
     * 获取展品列表
     * @return
     */
    public PageResult<MsCollection> listMsCollectionTop(PageQuery pageQuery) {
        // 热门展品
        if("热门藏品".equals(pageQuery.getMenuName())) {
            Page<MsCollection> page = lambdaQuery().orderByDesc(MsCollection::getViewCnt).page(pageQuery.toMpPage());
            return PageResult.of(page, page.getRecords());
        } else if("藏品一览".equals(pageQuery.getMenuName())) {
            Page<MsCollection> page = lambdaQuery().orderByDesc(MsCollection::getCrtTm).page(pageQuery.toMpPage());
            return PageResult.of(page, page.getRecords());
        }
        return null;
    }

    /**
     * 编辑展品
     * @param collection
     */
    public void editColl(MsCollection collection) {
        collection.setCrtTm(StringUtils.getNowDateTIme());
        saveOrUpdate(collection);
    }
    /**
     * 添加展品
     * @param collection
     */
    public void addColl(MsCollection collection) {
        collection.setCrtTm(StringUtils.getNowDateTIme());
        save(collection);
    }

    /**
     * 根据ID删除展品
     * @param id
     * @throws Exception
     */
    public void delColl(Integer id) throws Exception {
        MsCollection msCollection = baseMapper.selectById(id);
        if(msCollection == null) {
            throw new Exception("找不到原始记录，删除失败！");
        }
        removeById(id);
    }



}
