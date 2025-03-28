package com.museum.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.config.ExcepUtil;
import com.museum.config.JsonResult;
import com.museum.config.PageResult;
import com.museum.domain.dto.MsUserDTO;
import com.museum.domain.dto.ReserveQuery;
import com.museum.domain.po.MsReserve;
import com.museum.domain.po.MsReserveDetail;
import com.museum.mapper.CollectionMapper;
import com.museum.mapper.ReserveCollectionMapper;
import com.museum.mapper.ReserveDetialMapper;
import com.museum.mapper.ReserveMapper;
import com.museum.utils.RedisIdWorker;
import com.museum.utils.UserHolder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.museum.constants.Constant.LOCK_ADD_RESERVE;
import static com.museum.constants.Constant.CACHE_RESERVE_STOCK;

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
    @Resource
    ReserveService reserveService;

    //加载LUA脚本
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT;
    static {
        RESERVE_SCRIPT = new DefaultRedisScript<>();
        RESERVE_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource("reserve.lua")));
        RESERVE_SCRIPT.setResultType(Long.class);
    }


    //线程池
    private static ExecutorService reserve_order_executor = Executors.newSingleThreadExecutor();



    //类初始化后执行
    @PostConstruct
    private void init(){
        // 启动 g1 的消费者
        for (int i = 0; i < 5; i++) {
            String consumerName = "g1_c" + (i + 1);
            reserve_order_executor.submit(new ReserveOrderHandler("g1", consumerName));
        }

        // 启动 g2 的消费者
        for (int i = 0; i < 5; i++) {
            String consumerName = "g2_c" + (i + 1);
            reserve_order_executor.submit(new ReserveOrderHandler("g2", consumerName));
        }
    }
    private class ReserveOrderHandler implements Runnable {
        private final String groupName;
        private final String consumerName;
        private final String queueName = "stream.orders";

        public ReserveOrderHandler(String groupName, String consumerName) {
            this.groupName = groupName;
            this.consumerName = consumerName;
        }
        //TODO 延迟队列保证超时自动取消,超时的预约请求()等等
        @SneakyThrows
        @Override
        public void run() {
            while(true)
            {
                try {
                    //1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS streams.order >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(//这个方法支持返回多个流，list里有多个流。
                            Consumer.from(groupName, consumerName),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    //2.判断是否有消息
                    if(list==null||list.isEmpty())
                    {
                        //list为空，则开始下一次循环
                        continue;
                    }
                    //有消息，解析消息
                    MapRecord<String, Object, Object> record = list.get(0);//这里只有stream.orders，取一个就行;数据类型为 流名称，消息ID，消息内容
                    Map<Object, Object> values = record.getValue();
                    //3.解析成msReserveDetail数据
                    MsReserveDetail msReserveDetail = BeanUtil.fillBeanWithMap(values, new MsReserveDetail(), true);
                    //4.处理预约订单
                    handleReserveOrder(msReserveDetail);
                    //5.ACK确认
                    stringRedisTemplate.opsForStream().acknowledge(queueName,groupName,record.getId());
                } catch (Exception e) {
                    log.debug("处理预约订单异常",e);
                    handlePendingList();
                }
            }
        }
        private void handlePendingList() {
            while(true)
            {
                try {
                    //1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS streams.order >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(//这个方法支持返回多个流，list里有多个流。
                            Consumer.from(groupName, consumerName),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    //2.判断是否有消息
                    if(list==null||list.isEmpty())
                    {
                        //list为空，则无需处理
                        break;
                    }
                    //有消息，解析消息
                    MapRecord<String, Object, Object> record = list.get(0);//这里只有stream.orders，取一个就行;数据类型为 流名称，消息ID，消息内容
                    Map<Object, Object> values = record.getValue();
                    //3.解析成msReserveDetail数据
                    MsReserveDetail msReserveDetail = BeanUtil.fillBeanWithMap(values, new MsReserveDetail(), true);
                    //4.处理预约订单
                    handleReserveOrder(msReserveDetail);
                    //5.ACK确认
                    stringRedisTemplate.opsForStream().acknowledge(queueName,groupName,record.getId());
                } catch (Exception e) {
                    log.debug("处理Pending-list异常",e);
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }



    /**
     * 用于处理、生成后台预约订单
     * @param msReserveDetail
     */
    private void handleReserveOrder(MsReserveDetail msReserveDetail) {
        // 获取分布式锁
        String lockKey = LOCK_ADD_RESERVE + msReserveDetail.getUserId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 获取锁并处理
            lock.lock();
            createReserveDetail(msReserveDetail);

        } catch (Exception e) {
            // 异常时需要回滚Redis
            stringRedisTemplate.opsForValue().increment(CACHE_RESERVE_STOCK + msReserveDetail.getResId());
            stringRedisTemplate.opsForSet().remove(
                "cache:Reserve:Order:" + msReserveDetail.getResId(),
                msReserveDetail.getUserId()
            );
            log.error("预约处理异常，已回滚Redis数据", e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 添加预约详情，通过异步方式(消息队列）
     */
    public JsonResult addDetailAsync(MsReserveDetail detail) throws Exception {
        //首先得到该预约单据的全局（流水线）ID,不使用自增ID，涉及到的问题请参考全局ID的优势
        long orderId = redisIdWorker.nextId("order");
        detail.setOrderId(orderId);
        //执行LUA脚本
        Long result = stringRedisTemplate.execute(
                RESERVE_SCRIPT,
                Collections.emptyList(),
                detail.getResId().toString(),
                detail.getUserId(),
                String.valueOf(orderId),
                detail.getResDate(),
                detail.getUserName() 
        );
        int r=result.intValue();
        if(r!=0)
        {
            return JsonResult.failResult(r==1?"预约人数已满":"您已经预约过");
        }

        return JsonResult.result(orderId);
    }

    /**
     * 创建预约订单
     * @param msReserveDetail
     */
    @Transactional
    public void createReserveDetail(MsReserveDetail msReserveDetail)
    {
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
        
        // 使用乐观锁进行预扣减库存
        boolean success = reserveService.update()
                .setSql("resd_sum=resd_sum+1")
                .eq("id", reserve.getId())
                .lt("resd_sum", reserve.getResSum())
                .update();
        
        // 检查乐观锁更新是否成功
        if (!success) {
            log.info("使用乐观锁更新失败，预约人数可能已被其他线程更新，展览ID: {}", reserve.getId());
            return;
        }

        log.info("使用乐观锁成功更新预约人数，展览ID: {}, 用户ID: {}", reserve.getId(), msReserveDetail.getUserId());
        
        //4.更新成功，构造并保存完整预约记录
        msReserveDetail.setVldStat("1");
        msReserveDetail.setResType(reserve.getResTyp());
        msReserveDetail.setResDate(reserve.getResDate());
        msReserveDetail.setResTime(reserve.getResTime());
        msReserveDetail.setResSession(reserve.getResSession());
        save(msReserveDetail);
    }



/**
     添加预约详情，通过异步方式(阻塞队列）

    public JsonResult addDetailAsyncBlocked(MsReserveDetail detail) throws Exception {
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
    }**/

    /**
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
     **/
/**    //阻塞队列
 private BlockingQueue<MsReserveDetail> orderTasks = new ArrayBlockingQueue<>(1024*1024);**/
}
