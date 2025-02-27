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
        // 获取请求路径
        String requestURI = request.getRequestURI();
        
        // 对于 /user/me 接口，即使没有 token 也允许访问，但不设置 UserHolder
        if (requestURI.endsWith("/user/me")) {
            // 尝试获取 token
            String authorization = request.getHeader("Authorization");
            if (StrUtil.isBlank(authorization)) {
                // 未登录用户访问 /user/me，直接放行，不设置 UserHolder
                return true;
            }
            
            // 有 token，继续正常处理
        }
        
        //1.获取请求头中的token
        String authorization = request.getHeader("Authorization");
        if(StrUtil.isBlank(authorization)){
            //不存在，拦截
            response.setStatus(401);
            // 不要重定向，让前端处理401状态码
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
            // 不要重定向，让前端处理401状态码
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
