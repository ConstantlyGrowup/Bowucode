package com.museum.utils;


import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

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

}
