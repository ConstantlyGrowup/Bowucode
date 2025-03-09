package com.museum.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.museum.constants.Constant.*;

/**
 * 用户实现类。包括增删改查
 */
@Service
public class UserService extends ServiceImpl<MsUserMapper, MsUser> implements IService<MsUser> {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
    public String login(MsUser user) {
       /*
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
        }*/
        //1.得到用户名和密码，根据用户名去数据库里找是否存在该用户
        QueryWrapper<MsUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MsUser::getUsername,user.getUsername());
        //得到用户名结果
        MsUser msUser = baseMapper.selectOne(queryWrapper);
        //1.1不存在，直接返回null
        if(msUser==null)
        {
            return null;
        }else {
            //1.2存在，对照用户是否被封禁，再对照密码是否正确
            if(msUser.getState()==1){
                //被封禁
                return USER_BLOCKED;
            }
            if(!msUser.getPassword().equals(user.getPassword()))
            {
                //1.2.1密码不正确，返回null
                return null;
            }
            //2.密码正确，保存用户信息到redis
            //3.随机生成token,作为登录令牌
            String token = UUID.randomUUID().toString();
            //4.将user对象转为hashmap存储（只有面临多个线程修改同一对象才考虑线程安全）
            MsUserDTO userDTO = BeanUtil.copyProperties(msUser, MsUserDTO.class);
            Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                    CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldname, fieldvalue) -> fieldvalue.toString()));
            //将userDTO存储在ThredLocal中
            //5.存储
            String tokenKey=LOGIN_USER_KEY+token;
            stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
            //设置有效期
            stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL, TimeUnit.MINUTES);
            return token;
        }
    }

    /**
     * 添加一个用户，时间自动生成
     * @param user
     */
    public void saveMsUser(MsUser user, HttpSession session) throws Exception {
        //判断用户名是否存在
        QueryWrapper<MsUser> queryWrapper = new QueryWrapper<>();
        MsUser msUser = baseMapper.selectOne(queryWrapper.lambda().eq(MsUser::getUsername, user.getUsername()));
        if(msUser!=null)
        {
            ExcepUtil.throwErr("用户名已存在，创建失败！");
        }
        user.setState(0);
        user.setDate(StringUtils.getNowDate());
        save(user);
    }


    /**
     * 删除用户，根据ID
     * @param id
     */
    public void deluser(Integer id) {
        removeById(id);
    }

    /**
     * 更新用户信息
     * @param user 用户信息对象
     * @throws Exception 如果更新失败
     */
    public void updateUser(MsUser user) throws Exception {
        if(user.getId() == null) {
            throw new Exception("用户ID不能为空");
        }
        
        // 检查用户是否存在
        MsUser existingUser = getById(user.getId());
        if(existingUser == null) {
            throw new Exception("找不到指定ID的用户");
        }
        
        // 更新用户信息
        boolean success = updateById(user);
        if(!success) {
            throw new Exception("更新用户信息失败");
        }
    }

}
