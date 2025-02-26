package com.museum.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.config.ExcepUtil;
import com.museum.config.PageResult;
import com.museum.domain.dto.MsUserDTO;
import com.museum.domain.po.MsUser;
import com.museum.domain.query.PageQuery;
import com.museum.mapper.MsUserMapper;
import com.museum.utils.StringUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
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
    public MsUserDTO login(MsUser user, HttpSession session) {
        QueryWrapper<MsUser> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.lambda().eq(MsUser::getUsername, user.getUsername());
        if(StringUtils.isNotBlank(user.getPassword())) {
            userQueryWrapper.lambda().eq(MsUser::getPassword, user.getPassword());
        }
        List<MsUser> users = baseMapper.selectList(userQueryWrapper);
        MsUser msUser = users.get(0);
        //得到user，转为userDTO存储，存储在session中
        MsUserDTO userDTO = BeanUtil.copyProperties(msUser, MsUserDTO.class);
        if(!users.isEmpty()) {
            session.setAttribute("user", user);
            return userDTO;
        }else {
            return null;
        }
    }

    /**
     * 添加一个用户，时间自动生成
     * @param user
     */
    public void saveMsUser(MsUser user, HttpSession session) throws Exception {
        MsUser dbUser = new MsUser();
        dbUser.setUsername(user.getUsername());
        if(null != login(dbUser,session)) {
            ExcepUtil.throwErr("用户名已存在，创建失败！");
        }
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

    public MsUser getuser(Integer id){
        return baseMapper.selectById(id);
    }
}
