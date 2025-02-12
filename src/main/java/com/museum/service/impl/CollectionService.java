package com.museum.service.impl;

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
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 * @since 2023-22-29
 */
@Service
public class CollectionService extends ServiceImpl<CollectionMapper, MsCollection> implements IService<MsCollection> {

    /**
     * 获取展品列表
     * @return
     */
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
