package com.museum.utils;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.museum.service.impl.CollectionService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonBloomFilter;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.museum.constants.Constant.CACHE_NULL_TTL;

@Slf4j
@Component
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 针对空值
     * @param key
     * @param value
     * @param time
     * @param timeUnit
     */
    public void set(String key,Object value,Long time,TimeUnit timeUnit)
    {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,timeUnit);
    }

    /**
     * 存储逻辑过期
     * @param key
     * @param value
     * @param time
     * @param timeUnit
     */
    public void setWithLogicalExpire(String key,Object value,Long time,TimeUnit timeUnit)
    {
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        redisData.setData(value);
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    //开启独立线程需要线程池

    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);
    /**
     * 结合布隆过滤器的藏品查询(以藏品情景为例)
     * @param keyPrefix
     * @param lockPrefix
     * @param id
     * @param type
     * @param dbFallBack
     * @param time
     * @param timeUnit
     * @return
     * @param <R>
     * @param <I>
     */
    public <R,I> R queryWithBloomAndLogical(String keyPrefix, String lockPrefix,String BloomPrefix, I id, Class<R> type, Function<I,R> dbFallBack,Long time,TimeUnit timeUnit)
    {
        String key=keyPrefix+id;String lockKey=lockPrefix+id;
        //获取布隆过滤器(布隆过滤器需要初始化)
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(BloomPrefix);
        if(!bloomFilter.contains(id))
        {
            //id不在布隆过滤器，那么一定不在藏品列表
            log.info("布隆过滤器中id{}不存在",id);
            return null;
        }
        //id存在布隆过滤器，可能误判，下一步
        //先查找缓存
        String cate = stringRedisTemplate.opsForValue().get(key);
        //如果有空值也算blank,即空值、无值
        if(StrUtil.isNotBlank(cate))
        {
            //当命中缓存(即缓存中有内容，非空值）
            //判断是否逻辑过期
            //反序列化cate
            RedisData redisData = JSONUtil.toBean(cate, RedisData.class);
            //得到逻辑过期时间和藏品数据
            LocalDateTime logicalTime = redisData.getExpireTime();
            Object cateData = redisData.getData();
            R r = BeanUtil.toBean(cateData, type);
            if(logicalTime.isAfter(LocalDateTime.now()))
            {
                //如果没过期,返回商铺信息
                log.info("逻辑未过期，返回id{} 实体数据",id);
                return r;
            }else
            {
                //逻辑过期,重建缓存
                //尝试获取分布式锁
                RLock lock = redissonClient.getLock(lockKey);
                boolean lockSuccess = lock.tryLock();
                if(lockSuccess)
                {
                    //获取分布式锁成功,开启独立线程
                    CACHE_REBUILD_EXECUTOR.submit(()->{
                        try {
                            //查询数据库，并写入缓存
                            R r1 = dbFallBack.apply(id);
                            //写入缓存
                            this.setWithLogicalExpire(key,r1,time,timeUnit);
                            log.info("id{} 实体缓存重建成功！",id);
                        } catch (Exception e) {
                            log.error("ID{} 的实体缓存重建失败", id, e);
                        } finally {
                            //释放分布式锁
                            lock.unlock();
                            log.info("实体id{} 的分布式锁已解锁",id);
                        }
                    });
                    //返回过期的物品信息
                    return r;
                }
            }

        }
        //未命中缓存情况(空值、无值）
        //先判断空值
        if(cate!=null)
        {
            //如果是空值
            return null;
        }
        //如果是无值
        //根据id查询数据库
        R r = dbFallBack.apply(id);
        if(r!=null)
        {
            //如果该物品存在
            this.setWithLogicalExpire(key,r,time,timeUnit);
            log.info("该物品id{} 第一次建立逻辑过期",id);
            return r;
        }
        //如果该物品不存在
        //缓存写入空值
        log.info("物品id{} 不存在！写入空值",id);
        this.set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
        return null;
    }

}
