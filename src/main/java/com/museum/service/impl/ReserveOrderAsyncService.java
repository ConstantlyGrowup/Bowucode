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
import com.museum.utils.RedisIdWorker;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.museum.constants.Constant.LOCK_ADD_RESERVE;

/**
 * <p>
 *  服务实现类
 * </p>
 * @since 2023-22-29
 */
@Service
@Slf4j
public class ReserveOrderAsyncService extends ServiceImpl<ReserveDetialMapper, MsReserveDetail> implements IService<MsReserveDetail> {

    @Resource
    ReserveMapper reserveMapper;
    @Resource
    RedissonClient redissonClient;
    @Resource
    RedisIdWorker redisIdWorker;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    //加载LUA脚本
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT;
    static {
        RESERVE_SCRIPT = new DefaultRedisScript<>();
        RESERVE_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource("reserve.lua")));
        RESERVE_SCRIPT.setResultType(Long.class);
    }
    //阻塞队列
    private BlockingQueue<MsReserveDetail> orderTasks = new ArrayBlockingQueue<>(1024*1024);
    //线程池
    private static ExecutorService reserve_order_executor = Executors.newFixedThreadPool(10);
    //类初始化后执行
    @PostConstruct
    private void init(){
        reserve_order_executor.submit(new ReserveOrderHandler());
    }
    //处理阻塞队列
    private class ReserveOrderHandler implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            while (true){
                try {
                    //1.获取队列中的预约流水线订单信息
                    MsReserveDetail msReserveDetail = orderTasks.take();
                    //2.创建用户预约流水线订单
                    handleReserveOrder(msReserveDetail);
                } catch (InterruptedException e) {
                    log.debug("处理订单异常",e);
                }
            }
        }
    }

    /**
     * 用于处理、生成后台预约订单
     * @param msReserveDetail
     */
    private void handleReserveOrder(MsReserveDetail msReserveDetail) {
        //在处理这一步，redis部分各种前置条件已经满足,则需保证一人一单
        String lockKey = LOCK_ADD_RESERVE + msReserveDetail.getResId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock();
            //如果获取锁不成功
            if (!locked) {
                log.info("系统繁忙！");
                return;
            }
            //获取锁成功,后台建立预约单据
            //redis部分满足，看数据库满不满足一人一单,是否过期等

            //1.检查日期，预约是否过期
            //当天日期
            LocalDate now = LocalDate.now();
            //日期转换
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate resDate = LocalDate.parse(msReserveDetail.getResDate(), formatter);
            //如果已经过期
            if (resDate.isBefore(now)) {
                log.info("不能预约过期展览！");
                return;
            }
            //2.检查是否还能继续预约人
            MsReserve reserve = reserveMapper.selectById(msReserveDetail.getResId());//得到单个展览
            Integer currentSum = reserve.getResdSum();
            if (currentSum + 1 > reserve.getResSum())//如果加上当前预约数大于预约总数
            {
                log.info("预约人数超限！");
                return;
            }
            //3.检查是否先前有过预约
            QueryWrapper<MsReserveDetail> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda()
                    .eq(MsReserveDetail::getUserId, msReserveDetail.getUserId())
                    .eq(MsReserveDetail::getResId, msReserveDetail.getResId())
                    .eq(MsReserveDetail::getVldStat, "1");

            if (count(queryWrapper) > 0) {
                log.info("该用户已存在预约记录！");
                return;
            }
            //4.条件均满足，保存预约记录
            msReserveDetail.setVldStat("1");
            save(msReserveDetail);
            // 预扣减库存并保存记录
            reserve.setResdSum(currentSum + 1);
            reserveMapper.updateById(reserve);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 添加预约详情，通过异步方式
     */
    public JsonResult addDetailAsync(MsReserveDetail detail) throws Exception {
        //执行LUA脚本
        Long result = stringRedisTemplate.execute(
            RESERVE_SCRIPT,
            Collections.emptyList(),
            detail.getResId().toString(),
            detail.getUserId().toString()
        );
        int r=result.intValue();
        if(r!=0)
        {
            return JsonResult.failResult(r==1?"预约人数已满":"您已经预约过");
        }
        //有预约资格，可以将下单信息保存到阻塞队列
        //首先得到该预约单据的全局（流水线）ID,不使用自增ID，涉及到的问题请参考全局ID的优势
        long orderId = redisIdWorker.nextId("order");
        detail.setId(orderId);
        //将这个预约订单丢到阻塞队列里
        orderTasks.add(detail);
        return JsonResult.result(orderId);
    }


}
