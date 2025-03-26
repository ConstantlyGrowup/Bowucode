package com.museum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.museum.domain.po.MsReserveDetail;
import com.museum.mapper.ReserveDetialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReserveExpirationService {

    @Resource
    private ReserveDetialMapper reserveDetialMapper;
    
    /**
     * 服务启动时执行一次
     */
    @PostConstruct
    public void initCheck() {
        log.info("服务启动时执行预约过期检查...");
        checkExpired();
    }
    
    /**
     * 每天凌晨2点执行
     * cron表达式格式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkExpiredReservations() {
        log.info("定时任务执行预约过期检查...");
        checkExpired();
    }
    
    /**
     * 检查过期预约的公共方法
     */
    private void checkExpired() {
        try {
            // 获取当前日期
            LocalDate today = LocalDate.now();
            String currentDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 查询所有有效且日期早于今天的预约
            LambdaQueryWrapper<MsReserveDetail> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MsReserveDetail::getVldStat, "1")  // 有效预约
                        .lt(MsReserveDetail::getResDate, currentDate);  // 预约日期早于今天
            
            List<MsReserveDetail> expiredReservations = reserveDetialMapper.selectList(queryWrapper);
            
            log.info("找到 {} 条过期预约", expiredReservations.size());
            
            // 将过期预约标记为无效
            for (MsReserveDetail reservation : expiredReservations) {
                reservation.setVldStat("0");  // 设置为无效
                reserveDetialMapper.updateById(reservation);
                log.info("标记过期预约 ID: {}, 用户: {}, 日期: {}", 
                         reservation.getOrderId(),
                         reservation.getUserName(), 
                         reservation.getResDate());
            }
            
            log.info("过期预约检查完成，共处理 {} 条记录", expiredReservations.size());
        } catch (Exception e) {
            log.error("检查过期预约时发生错误", e);
        }
    }
} 