package com.museum.config;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.museum.domain.dto.MsUserDTO;
import com.museum.domain.po.MsUser;
import com.museum.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.museum.constants.Constant.LOGIN_USER_KEY;
import static com.museum.constants.Constant.LOGIN_USER_TTL;

public class LoginInterceptor implements HandlerInterceptor {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    public LoginInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        //获取session
//        HttpSession session = request.getSession();
//        //获取用户
//        MsUser user = (MsUser) session.getAttribute("user");
//        //判断用户是否存在
//        if(user==null){
//            //response.setStatus(401);
//            // 重定向到登录页面
//            response.sendRedirect("/login.html");
//            return false;
//        }
//        MsUserDTO userDTO = new MsUserDTO();
//        BeanUtils.copyProperties(user,userDTO);
//        //存在，保存用户信息的ThreadLocal
//        UserHolder.saveUser(userDTO);
//        //放行
//        return true;
        //1.获取请求头中的token
        // 获取Authorization头部中的token
        String authorization = request.getHeader("Authorization");
        if(StrUtil.isBlank(authorization)){
            //不存在，拦截
            response.setStatus(401);
            return false;
        }

        // 检查并移除Bearer前缀
        String token = authorization;
        if(authorization.startsWith("Bearer ")){
            token = authorization.substring(7);
        }

        if(StrUtil.isBlank(token)){
            //token不合法，拦截
            response.setStatus(401);
            return false;
        }
        //2.基于TOKEN获取redis的用户
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        //判断用户是否存在
        if(userMap.isEmpty())
        {
            //不存在，拦截
            response.setStatus(401);
            response.sendRedirect("login.html");
            return false;
        }
        //3.将查询到的Hash数据转化为UserDTO对象
        MsUserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new MsUserDTO(), false);
        //4.存在，保存用户信息的ThreadLocal
        UserHolder.saveUser(userDTO);
        //5.刷新token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY + token,LOGIN_USER_TTL, TimeUnit.MINUTES);
        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.removeUser();
    }
}
