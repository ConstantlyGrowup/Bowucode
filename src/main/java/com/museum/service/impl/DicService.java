package com.museum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.damain.po.MsDic;
import com.museum.mapper.DicMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @since 2023-12-19
 */
@Service
public class DicService extends ServiceImpl<DicMapper, MsDic> implements IService<MsDic> {

    /**
     * 获取字典值类型
     * @return
     */
    public List<String> listDicTyp() {
        List<MsDic> dics = list();
        Set<String> typs = new HashSet<>();
        for(MsDic dic : dics) {
            typs.add(dic.getDicTyp()); // 利用set的去重机制，只保留字典值类型
        }
        return new ArrayList<>(typs);
    }

    /**
     * 获取字典值。根据类型
     * @return
     */
    public List<MsDic> listDicByTyp(String dicTyp) {
        QueryWrapper<MsDic> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MsDic::getDicTyp, dicTyp);
        return list(queryWrapper);
    }
    /**
     * 获取字典值。根据类型
     * @return
     */
    public List<String> listDicValueByTyp(String dicTyp) {
        List<MsDic> datas = listDicByTyp(dicTyp);
        List<String> values = new ArrayList<>(datas.size());
        for(MsDic msDic: datas) {
            values.add(msDic.getDicValue());
        }
        return values;
    }

    /**
     * 保存字典值
     * @return
     */
    public void addDic(MsDic dic) throws Exception {
        if(StringUtils.isBlank(dic.getDicTyp()) || StringUtils.isBlank(dic.getDicValue()) ) {
            throw new Exception("类型与字典值不允许为空！");
        }
        // 检查一下是不是存在了，如果存在不允许删除
        QueryWrapper<MsDic> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MsDic::getDicTyp, dic.getDicTyp()).eq(MsDic::getDicValue, dic.getDicValue());
        List<MsDic> data = list(queryWrapper);
        if(data.size() > 0) {
            throw new Exception("字典值已存在，不允许重复新增！");
        }
        save(dic);
    }

    /**
     * 获取字典值。Id
     * @return
     */
    public void delDic(Integer id) {
        removeById(id);
    }
}
