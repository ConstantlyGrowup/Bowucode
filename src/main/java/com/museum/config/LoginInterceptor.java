package com.museum.config;

import com.museum.domain.dto.MsUserDTO;
import com.museum.domain.po.MsUser;
import com.museum.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取session
        HttpSession session = request.getSession();
        //获取用户
        MsUser user = (MsUser) session.getAttribute("user");
        //判断用户是否存在
        if(user==null){
            //response.setStatus(401);
            // 重定向到登录页面
            response.sendRedirect("/login.html");
            return false;
        }
        MsUserDTO userDTO = new MsUserDTO();
        BeanUtils.copyProperties(user,userDTO);
        //存在，保存用户信息的ThreadLocal
        UserHolder.saveUser(userDTO);
        //放行
        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.removeUser();
    }
}
