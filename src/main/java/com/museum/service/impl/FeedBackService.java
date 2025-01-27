package com.museum.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.config.PageResult;
import com.museum.domain.dto.FeedBackQuery;
import com.museum.domain.po.FeedBack;
import com.museum.domain.query.PageQuery;
import com.museum.mapper.FeedBackMapper;
import com.museum.utils.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @since 2023-12-19
 */
@Service
public class FeedBackService extends ServiceImpl<FeedBackMapper, FeedBack> implements IService<FeedBack> {
    @Resource
    DicService dicService;

    /**
     * 获取留言信息列表。自动过滤敏感词
     * @return
     */
//    public PageResult<FeedBack> listFeedBackByUser(FeedBackQuery pageQuery) {
//        LambdaQueryChainWrapper<FeedBack> lambdaQueryChainWrapper = lambdaQuery().like(FeedBack::getFeedContent, pageQuery.getName());
//        if(StringUtils.isNotBlank(pageQuery.getUserName())) {
//            lambdaQueryChainWrapper.like(FeedBack::getUserName, pageQuery.getUserName());
//        }
//        if(null != pageQuery.getUserId()) {
//            lambdaQueryChainWrapper.eq(FeedBack::getUserId, pageQuery.getUserId());
//        }
//        if(null != pageQuery.getCateId()) {
//            lambdaQueryChainWrapper.eq(FeedBack::getCateId, pageQuery.getCateId());
//        }
//        List<String> keyWords = dicService.listDicValueByTyp("敏感词");
//        if(!keyWords.isEmpty()) {
//            for(String key: keyWords) {
//                lambdaQueryChainWrapper.notLike(FeedBack::getFeedContent, key);
//            }
//        }
//        Page<FeedBack> page = lambdaQueryChainWrapper.page(pageQuery.toMpPage());
//        return PageResult.of(page, page.getRecords());
//    }

    public PageResult<FeedBack> listFeedBackByUser(FeedBackQuery pageQuery) {
        LambdaQueryChainWrapper<FeedBack> lambdaQueryChainWrapper = lambdaQuery()
                .like(FeedBack::getFeedContent, pageQuery.getName())
                .eq(FeedBack::getIsShow, 1); // 增加过滤条件，只查询isShow为1的评论

        if(StringUtils.isNotBlank(pageQuery.getUserName())) {
            lambdaQueryChainWrapper.like(FeedBack::getUserName, pageQuery.getUserName());
        }
        if(null != pageQuery.getUserId()) {
            lambdaQueryChainWrapper.eq(FeedBack::getUserId, pageQuery.getUserId());
        }
        if(null != pageQuery.getCateId()) {
            lambdaQueryChainWrapper.eq(FeedBack::getCateId, pageQuery.getCateId());
        }
        List<String> keyWords = dicService.listDicValueByTyp("敏感词");
        if(!keyWords.isEmpty()) {
            for(String key: keyWords) {
                lambdaQueryChainWrapper.notLike(FeedBack::getFeedContent, key);
            }
        }
        Page<FeedBack> page = lambdaQueryChainWrapper.page(pageQuery.toMpPage());
        return PageResult.of(page, page.getRecords());
    }

    /**
     * 获取留言信息列表。不过滤敏感词
     * @return
     */
    public PageResult<FeedBack> listAllFeedBack(PageQuery pageQuery) {
        LambdaQueryChainWrapper<FeedBack> lambdaQueryChainWrapper = lambdaQuery().like(FeedBack::getFeedContent, pageQuery.getName());
        Page<FeedBack> page = lambdaQueryChainWrapper.page(pageQuery.toMpPage());
        return PageResult.of(page, page.getRecords());
    }

    /**
     * 添加用户留言
     * @return
     */
    public void addFeedBack(FeedBack feedBack) throws Exception {
        feedBack.setFedDateTime(StringUtils.getNowDateTIme());
        feedBack.setIsShow("1");
        save(feedBack);
        // 如果涉及到敏感词，则将其设置为不可展示
        List<String> keyWords = dicService.listDicValueByTyp("敏感词");
        LambdaQueryChainWrapper<FeedBack> lambdaQueryChainWrapper = lambdaQuery().eq(FeedBack::getId, feedBack.getId());
        if(!keyWords.isEmpty()) {
            for(String key: keyWords) {
                lambdaQueryChainWrapper.notLike(FeedBack::getFeedContent, key);
            }
        }
        List<FeedBack> da = baseMapper.selectList(lambdaQueryChainWrapper);
        if(null != da && da.size() ==1) {
            feedBack.setIsShow("0");
            saveOrUpdate(feedBack);
        }
    }

    /**
     * 删除用户留言
     * @return
     */
    public void delFeedBack(Integer id) {
        removeById(id);
    }

    /**
     * 编辑用户留言
     * @return
     */
    public void editFeedBack(FeedBack feedBack) {
        saveOrUpdate(feedBack);
    }
}
