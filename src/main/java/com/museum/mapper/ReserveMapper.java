package com.museum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.domain.po.MsReserve;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 *@since 2023-12-19
 */
@Mapper
public interface ReserveMapper extends BaseMapper<MsReserve> {

    @Select("SELECT DISTINCT r.* FROM ms_reserve r " +
            "LEFT JOIN ms_reserve_collection rc ON r.id = rc.reserve_id " +
            "LEFT JOIN ms_collection c ON rc.cate_id = c.id " +
            "WHERE c.title LIKE CONCAT('%', #{collectionName}, '%')")
    List<MsReserve> findByCollectionName(String collectionName);
}
