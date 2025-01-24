package com.museum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.config.ExcepUtil;
import com.museum.config.PageResult;
import com.museum.damain.po.MsUser;
import com.museum.damain.query.PageQuery;
import com.museum.mapper.MsUserMapper;
import com.museum.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户实现类。包括增删改查
 */
@Service
public class UserService extends ServiceImpl<MsUserMapper, MsUser> implements IService<MsUser> {

    /**
     * 列出所有用户。可以根据用户名模糊查询
     * @param query
     * @return
     */
    public PageResult<MsUser> listUserPage(PageQuery query) {
        Page<MsUser> page = lambdaQuery().like(MsUser::getUsername, query.getName())
                .page(query.toMpPage());
        return PageResult.of(page,page.getRecords());
    }
    /**
     * 用户登录
     * @param user
     * @return
     */
    public MsUser login(MsUser user) {
        QueryWrapper<MsUser> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.lambda().eq(MsUser::getUsername, user.getUsername());
        if(StringUtils.isNotBlank(user.getPassword())) {
            userQueryWrapper.lambda().eq(MsUser::getPassword, user.getPassword());
        }
        List<MsUser> users = baseMapper.selectList(userQueryWrapper);
        if(!users.isEmpty()) {
            return users.get(0);
        }else {
            return null;
        }
    }

    /**
     * 添加一个用户，时间自动生成
     * @param user
     */
    public void saveMsUser(MsUser user) throws Exception {
        MsUser dbUser = new MsUser();
        dbUser.setUsername(user.getUsername());
        if(null != login(dbUser)) {
            ExcepUtil.throwErr("用户名已存在，创建失败！");
        }
//        if(user.getState() == null) {
//            user.setState(0);
//        }
        user.setState(0);
        user.setDate(StringUtils.getNowDate());
        save(user);
    }

    /**
     * 编辑用户
     * @param user
     */
    public void editUserInfo(MsUser user) {
        user.setDate(StringUtils.getNowDate());
        this.saveOrUpdate(user);
    }

    /**
     * 删除用户，根据ID
     * @param id
     */
    public void deluser(Integer id) {
        removeById(id);
    }
}
