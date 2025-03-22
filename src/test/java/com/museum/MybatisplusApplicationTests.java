package com.museum;

import com.museum.domain.po.MsCollection;
import com.museum.service.impl.CollectionService;
import com.museum.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.museum.constants.Constant.BLOOM_COLLECT;

@SpringBootTest
@Slf4j
class MybatisplusApplicationTests {
    @Resource
    private RedissonClient redissonClient;
    @Autowired
    private CollectionService collectionService;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Test//初始化布隆过滤器，加载所有商铺id到布隆过滤器中
    void BloomWorker(){
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(BLOOM_COLLECT);
        //初始化布隆过滤器
        bloomFilter.tryInit(1000L,0.05);
        //从数据库里加载所有商铺id
        List<Integer> ids = this.loadAllIds();
        log.info("从数据库加载了 {} 个藏品ID", ids.size());
        //将所有id添加到布隆过滤器中
        ids.forEach(bloomFilter::add);
        log.info("布隆过滤器初始化完成，已加载 {} 个藏品ID", ids.size());
    }

    private List<Integer> loadAllIds(){
        // 1. 调用list方法，获取所有藏品的列表
        // 2. 将列表转换为Stream流，以便进行流式操作
        // 3. 使用map方法将每个对象映射为其ID
        // 4. 将流中的元素收集到一个List中
        return collectionService
                .list()
                .stream()
                .map(MsCollection::getId)
                .collect(Collectors.toList());
    }
    @Test
    //测试布隆过滤器是否管用
    void TestIfInBloom()
    {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(BLOOM_COLLECT);
        boolean ifTrue = bloomFilter.contains(10);
        log.info("id 10藏品是否在布隆过滤器中？{}",ifTrue);
    }

    private ExecutorService es=Executors.newFixedThreadPool(500);
    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch=new CountDownLatch(200);
        Runnable task=()->{
            for(int i=0;i<100;i++)
            {
                long orderId = redisIdWorker.nextId("orderTest");
                System.out.println("id=: "+orderId);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for(int i=0;i<200;i++)
        {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("Result Time = " + (end-begin));
    }
}