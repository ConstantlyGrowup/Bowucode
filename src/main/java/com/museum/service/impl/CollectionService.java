package com.museum.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.config.PageResult;
import com.museum.domain.dto.CollectionQuery;
import com.museum.domain.po.MsCollection;
import com.museum.domain.query.PageQuery;
import com.museum.mapper.CollectionMapper;
import com.museum.utils.CacheClient;
import com.museum.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.museum.constants.Constant.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 * @since 2023-22-29
 */
@Slf4j
@Service
@Transactional
public class CollectionService extends ServiceImpl<CollectionMapper, MsCollection> implements IService<MsCollection> {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;
    @Resource
    private RedissonClient redissonClient;



    // 创建线程池处理异步任务
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 获取coll-list该页面下所有藏品
     * @param pageQuery
     * @return
     */
    public PageResult<MsCollection> listMsCollectionList(CollectionQuery pageQuery)
    {
        // 创建Lambda查询构造器
        // 修改为条件查询，处理name模糊搜索和cateId精确匹配
        var query = lambdaQuery();
        
        // 处理name参数 - 模糊查询
        if (StringUtils.isNotEmpty(pageQuery.getName())) {
            query.like(MsCollection::getTitle, pageQuery.getName());
            log.info("按名称模糊查询: {}", pageQuery.getName());
        }
        
        // 处理cateId参数 - 精确匹配
        if (StringUtils.isNotEmpty(pageQuery.getCateId())) {
            query.eq(MsCollection::getCateId, pageQuery.getCateId());
            log.info("按分类精确查询: {}", pageQuery.getCateId());
        }
        
        // 执行分页查询
        Page<MsCollection> page = query.orderByDesc(MsCollection::getCrtTm)
                                      .page(pageQuery.toMpPage());
        
        return PageResult.of(page, page.getRecords());
    }
    

    /**
     * 获取单个藏品
     * @param pageQuery
     * @return
     */
    public MsCollection getMsCollection(CollectionQuery pageQuery) {
        //判断是否为空
        Integer collectId = pageQuery.getId();
        if(collectId == null) {
            return null;
        }


        // 获取分布式锁
        String lockKey = "Update_Cache_DB";
        RLock lock = redissonClient.getLock(lockKey);

        MsCollection msCollection = cacheClient
                .queryWithBloomAndLogical(CACHE_COLLECT, LOCK_COLLECT_KEY, BLOOM_COLLECT,
                        collectId, MsCollection.class,
                        id -> getById(id), CACHE_COLLECT_TTL, TimeUnit.HOURS);
        
        if(msCollection == null) {
            return null;
        }
        /*
        //同步策略
        // 更新点击数并同步缓存
        msCollection.setViewCnt(msCollection.getViewCnt() + 1);
        updateById(msCollection);

        // 更新缓存中的数据
        cacheClient.setWithLogicalExpire(
                CACHE_COLLECT + collectionId,
                msCollection,
                CACHE_COLLECT_TTL,
                TimeUnit.HOURS
        );*/
        // 异步更新点击数和缓存
        CACHE_REBUILD_EXECUTOR.submit(() -> {
            try {
                // 获取锁
                if (lock.tryLock(10, TimeUnit.SECONDS)) {
                    try {
                        // 更新点击数
                        msCollection.setViewCnt(msCollection.getViewCnt() + 1);
                        updateById(msCollection);
                        
                        // 更新缓存
                        cacheClient.setWithLogicalExpire(
                            CACHE_COLLECT + collectId,
                            msCollection,
                                CACHE_COLLECT_TTL,
                            TimeUnit.HOURS
                        );
                        log.info("异步更新id{}实体点击量和缓存: ", collectId);
                    } finally {
                        // 释放锁
                        lock.unlock();
                    }
                }
            } catch (Exception e) {
                log.error("异步更新id{}点击量和缓存失败: ", collectId, e);
            }
        });

        return msCollection;
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


    //以上为客户端相关方法
    /**
     * 编辑展品
     * @param collection
     */
    public void editColl(MsCollection collection) {
        try {
            Boolean success = updateCacheDB(collection);
            if(success)
            {
                log.info("更新id:{}藏品信息成功！",collection.getId());
            }else
            {
                log.info("更新id:{}藏品信息失败！",collection.getId());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * 添加展品
     * @param collection
     */
    public void addColl(MsCollection collection) {
        collection.setCrtTm(StringUtils.getNowDateTIme());
        save(collection);
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(BLOOM_COLLECT);
        bloomFilter.add(collection.getId());
        log.info("id:{} 已经被加入到布隆过滤器",collection.getId());
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
        //操作缓存
        stringRedisTemplate.delete(CACHE_COLLECT+id);
    }

    /**
     * 基于缓存的更新策略
     * @param collection
     * @return
     */
    public Boolean updateCacheDB(MsCollection collection)
    {
       //更新数据库，删除缓存
        //顺序为1.先操作数据库2.再删除缓存
        //检查数据库里有没有这个藏品
        MsCollection clt = getById(collection.getId());
        if(clt==null)
        {
            return false;
        }
        //如果有，先操作数据库
        clt.setCrtTm(StringUtils.getNowDateTIme());
        saveOrUpdate(clt);
        //删除缓存
        stringRedisTemplate.delete(CACHE_COLLECT+collection.getId());
        return true;
    }


}
