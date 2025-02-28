package com.museum.utils;

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
        //1.判断ThredLocal中是否有用户
        String requestURI = request.getRequestURI();
        if(UserHolder.getUser()==null)
        {
            //对me接口进行特殊处理
            if(requestURI.endsWith("/user/me"))
            {
                //说明是无登录状态，但是调用了me接口，放行
                return true;
            }
            //没有用户，拦截
            response.setStatus(401);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.removeUser();
    }
}
