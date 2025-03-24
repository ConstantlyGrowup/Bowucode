package com.museum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.config.ExcepUtil;
import com.museum.config.PageResult;
import com.museum.domain.dto.ReserveQuery;
import com.museum.domain.po.MsCollection;
import com.museum.domain.po.MsReserve;
import com.museum.domain.po.MsReserveCollection;
import com.museum.domain.po.MsReserveDetail;
import com.museum.domain.query.PageQuery;
import com.museum.mapper.CollectionMapper;
import com.museum.mapper.ReserveCollectionMapper;
import com.museum.mapper.ReserveDetialMapper;
import com.museum.mapper.ReserveMapper;
import com.museum.utils.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.museum.constants.Constant.CACHE_RESERVE_STOCK;

/**
 * <p>
 *  服务实现类
 * </p>
 * @since 2023-22-29
 */
@Service
@Slf4j
public class ReserveService extends ServiceImpl<ReserveMapper, MsReserve> implements IService<MsReserve> {
    @Resource
    private ReserveDetialMapper reserveDetialMapper;
    @Resource
    CollectionMapper collectionMapper;
    @Resource
    private ReserveCollectionMapper reserveCollectionMapper;
    @Resource
    private ReserveMapper reserveMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 获取展览列表（后台管理使用）
     * @param pageQuery 分页查询参数
     * @return 分页结果
     */
    public PageResult<MsReserve> listMsReserve(ReserveQuery pageQuery) {
        // 构建查询条件
        LambdaQueryChainWrapper<MsReserve> queryChain = lambdaQuery();
        Integer cateId = pageQuery.getCateId();
        
        // 如果有名称搜索条件，添加模糊查询
        if (StringUtils.isNotBlank(pageQuery.getName())) {
            queryChain.like(MsReserve::getTitle, pageQuery.getName());
        }
        
        // 如果有藏品ID搜索条件，先查询关联表获取相关的预约ID列表
        List<Integer> reserveIds = null;
        if (cateId != null) {
            reserveIds = reserveCollectionMapper.findReserveIdsByCateId(cateId);
            if (reserveIds.isEmpty()) {
                // 如果没有找到相关预约，直接返回空结果
                return PageResult.of(new Page<>(), new ArrayList<>());
            }
            // 使用查询到的预约ID列表过滤
            queryChain.in(MsReserve::getId, reserveIds);
        }
        
        // 执行分页查询
        Page<MsReserve> page = queryChain.orderByDesc(MsReserve::getCrtTm).page(pageQuery.toMpPage());
        List<MsReserve> records = page.getRecords();
        
         // 查询每个展览关联的藏品信息和当前预约人数
        for (MsReserve reserve : records) {
            // 获取展览关联的藏品ID列表
            List<Integer> cateIds = reserveCollectionMapper.findCateIdsByReserveId(reserve.getId());
            if (!cateIds.isEmpty()) {
                // 查询藏品详细信息
                List<MsCollection> collections = collectionMapper.selectBatchIds(cateIds);
                reserve.setCollections(collections);
                reserve.setCateIds(cateIds.toArray(new Integer[0]));
            }
            
            // 查询当前预约人数
            QueryWrapper<MsReserveDetail> detailQuery = new QueryWrapper<>();
            detailQuery.lambda()
                    .eq(MsReserveDetail::getResId, reserve.getId())
                    .eq(MsReserveDetail::getVldStat, "1");
            int currentReserveCount = reserveDetialMapper.selectCount(detailQuery);
            reserve.setResdSum(currentReserveCount);
            reserveMapper.updateById(reserve);
        }
        
        return PageResult.of(page, records);
    }

    /**
     * 客户端过滤过时的预约，并显示预约
     * @param pageQuery
     * @return
     */
    public PageResult<MsReserve> listMsReserveClient(ReserveQuery pageQuery) {
        LocalDate today = LocalDate.now();//今日的日期
        String todayString = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

        
        // 如果是按藏品ID查询，先获取关联的展览ID
        List<Integer> reserveIds = null;
        if (pageQuery.getCateId() != null) {
            reserveIds = reserveCollectionMapper.findReserveIdsByCateId(pageQuery.getCateId());
            if (reserveIds.isEmpty()) {
                return PageResult.of(new Page<>(), new ArrayList<>());
            }
        }
        
        QueryWrapper<MsReserve> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .in(reserveIds != null, MsReserve::getId, reserveIds)
                .ge(MsReserve::getResDate, todayString);

        Page<MsReserve> page = this.page(pageQuery.toMpPage(), queryWrapper);
        
        // 获取每个展览关联的藏品信息及已预约人数
        List<MsReserve> records = page.getRecords();
        for (MsReserve reserve : records) {
            List<Integer> cateIds = reserveCollectionMapper.findCateIdsByReserveId(reserve.getId());
            reserve.setCateIds(cateIds.toArray(new Integer[0]));

             // 查询当前预约人数
            QueryWrapper<MsReserveDetail> detailQuery = new QueryWrapper<>();
            detailQuery.lambda()
                    .eq(MsReserveDetail::getResId, reserve.getId())
                    .eq(MsReserveDetail::getVldStat, "1");
            int currentReserveCount = reserveDetialMapper.selectCount(detailQuery);
            reserve.setResdSum(currentReserveCount);
            reserveMapper.updateById(reserve);
        }
        
        return PageResult.of(page, records);
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
        // 获取旧的预约信息，用于比较变更
        MsReserve oldReserve = getById(msReserve.getId());
        if (oldReserve == null) {
            throw new IllegalArgumentException("找不到要编辑的预约信息！");
        }
        
        // 设置更新时间
        msReserve.setCrtTm(StringUtils.getNowDateTIme());
        
        // 更新数据库中的记录
        saveOrUpdate(msReserve);
        
        // 如果总预约人数变更，需要更新Redis中的数据
        if (msReserve.getResSum() != null && !msReserve.getResSum().equals(oldReserve.getResSum())) {
            // 计算当前可用预约数量 = 总数 - 已预约数
            int availableCount = msReserve.getResSum() - (msReserve.getResdSum() != null ? msReserve.getResdSum() : oldReserve.getResdSum());
            
            // 更新Redis中的可用预约数量
            stringRedisTemplate.opsForValue().set(
                CACHE_RESERVE_STOCK + msReserve.getId(),
                String.valueOf(availableCount)
            );
            
            log.info("更新Redis中的预约信息，展览ID: {}, 可用预约数: {}", msReserve.getId(), availableCount);
        }
    }


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

        // 设置预约信息的其他属性
        msReserve.setResdSum(0);
        msReserve.setCrtTm(StringUtils.getNowDateTIme());
        //拼接展览标题
        String finalTitle = msReserve.getTitle() + "/" + msReserve.getResSession() + "/" + msReserve.getResTime();
        msReserve.setTitle(finalTitle);
        save(msReserve);

        //将这个预约信息存储到redis
        //如果是之后要修改、删除，涉及到数据一致性的逻辑
        //TODO 记得把每个预约信息都要重新修改，让它能在Redis中
        //TODO 做一个压测，和之前作对照
        stringRedisTemplate.opsForValue().set(CACHE_RESERVE_STOCK+msReserve.getId(),msReserve.getResSum().toString());

        // 关联多个藏品
        for (Integer cateId : cateIds) {
            MsCollection msCollection = collectionMapper.selectById(cateId);
            if (msCollection != null) {
                MsReserveCollection reserveCollection = new MsReserveCollection();
                reserveCollection.setReserveId(msReserve.getId());
                reserveCollection.setCateId(cateId);
                reserveCollectionMapper.insert(reserveCollection);
            } else {
                throw new IllegalArgumentException("指定的藏品ID不存在，请检查后重试！");
            }
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
        
        // 检查是否存在有效的预约记录
        QueryWrapper<MsReserveDetail> detialQueryWrapper = new QueryWrapper<>();
        detialQueryWrapper.lambda().eq(MsReserveDetail::getResId, id).eq(MsReserveDetail::getVldStat,"1");
        List<MsReserveDetail> data = reserveDetialMapper.selectList(detialQueryWrapper);
        if(!data.isEmpty()) {
            ExcepUtil.throwErr("该预约下存在有效的预约记录，无法删除！");
        }
        
        // 从数据库删除展览记录
        removeById(id);
        
        // 从Redis删除相关数据
        // 1. 删除库存记录
        stringRedisTemplate.delete(CACHE_RESERVE_STOCK + id);
        
        // 2. 删除展览对应的用户集合数据（该集合记录了哪些用户预约了该展览）
        String exhibitionUserKey = "cache:Reserve:Order:" + id;
        stringRedisTemplate.delete(exhibitionUserKey);
        
        log.info("删除Redis中的预约信息，展览ID: {}", id);
    }

    public List<MsReserveDetail> getUserReserve(Integer userId, Integer resId) {
        QueryWrapper<MsReserveDetail> detialQueryWrapper = new QueryWrapper<>();
        if(null != userId) {
            detialQueryWrapper.lambda().eq(MsReserveDetail::getUserId, userId);
        }
        if(null != resId) {
            detialQueryWrapper.lambda().eq(MsReserveDetail::getResId, resId);
        }
        List<MsReserveDetail> detials = reserveDetialMapper.selectList(detialQueryWrapper);
        return detials;
    }

    /**
     * 同步展览库存到Redis
     * 可以定时调用此方法，确保Redis与数据库保持一致
     */
    public void syncStockToRedis() {
        log.info("开始同步展览库存到Redis...");
        
        // 获取所有有效的展览信息
        List<MsReserve> reserveList = lambdaQuery()
                .ge(MsReserve::getResDate, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .list();
        
        for (MsReserve reserve : reserveList) {
            // 重新计算可用库存
            int availableStock = reserve.getResSum() - reserve.getResdSum();
            
            // 同步到Redis
            stringRedisTemplate.opsForValue().set(
                CACHE_RESERVE_STOCK + reserve.getId(),
                String.valueOf(Math.max(0, availableStock))
            );
            
            log.info("同步展览ID: {}, 总库存: {}, 已用: {}, 可用: {}", 
                    reserve.getId(), reserve.getResSum(), reserve.getResdSum(), availableStock);
        }
        
        log.info("展览库存同步完成，共同步{}个展览", reserveList.size());
    }
}
