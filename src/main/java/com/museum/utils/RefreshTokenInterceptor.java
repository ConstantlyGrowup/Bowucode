package com.museum.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.museum.domain.dto.MsUserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.museum.constants.Constant.LOGIN_USER_KEY;
import static com.museum.constants.Constant.LOGIN_USER_TTL;

public class RefreshTokenInterceptor implements HandlerInterceptor {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求头中的token
        String authorization = request.getHeader("Authorization");
        if (StrUtil.isBlank(authorization)) {
            return true;
        }

        // 检查并移除Bearer前缀
        String token = authorization;
        if (authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        if (StrUtil.isBlank(token)) {
            return true;
        }

        // 2.基于TOKEN获取redis中的用户
        String tokenKey = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);

        // 3.判断用户是否存在
        if (userMap.isEmpty()) {
            return true;
        }

        // 5.将查询到的Hash数据转变为UserDTO对象
        MsUserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new MsUserDTO(), false);

        // 6.将UserDTO对象存储到ThreadLocal中
        UserHolder.saveUser(userDTO);

        // 7.刷新TOKEN有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 8.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }
}
