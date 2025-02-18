package com.museum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.domain.po.MsReserveCollection;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface ReserveCollectionMapper extends BaseMapper<MsReserveCollection> {
    
    @Select("SELECT reserve_id FROM ms_reserve_collection WHERE cate_id = #{cateId}")
    List<Integer> findReserveIdsByCateId(Integer cateId);
    
    @Select("SELECT cate_id FROM ms_reserve_collection WHERE reserve_id = #{reserveId}")
    List<Integer> findCateIdsByReserveId(Integer reserveId);
}
