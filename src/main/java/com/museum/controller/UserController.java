package com.museum.controller;


import cn.hutool.core.util.StrUtil;
import com.museum.config.JsonResult;
import com.museum.config.PageResult;
import com.museum.domain.dto.MsUserDTO;
import com.museum.domain.po.MsUser;
import com.museum.domain.query.PageQuery;
import com.museum.service.impl.UserService;
import com.museum.utils.StringUtils;
import com.museum.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static com.museum.constants.Constant.LOGIN_USER_KEY;
import static com.museum.constants.Constant.USER_BLOCKED;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @since 2023-12-19
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    @Resource
    UserService userService;
    @Resource
    StringRedisTemplate stringRedisTemplate;

//    @PostMapping("/getdata")
//    public JsonResult getdata(@RequestBody PageQuery pageQuery) {
//        PageResult<MsUser> users = userService.listUserPage(pageQuery);
//        return JsonResult.result(users);
//    }
//    @PostMapping("/deluser")
//    public JsonResult deluser(@RequestBody MsUser msUser) {
//        try {
//            if(msUser.getId() == null) {
//                throw new Exception("ID不允许为空！！！");
//            }
//            userService.deluser(msUser.getId());
//            return JsonResult.result("成功！");
//        }catch (Exception e){
//            return JsonResult.failResult(e.getMessage());
//        }
//    }

    //以下为客户端的接口
    @PostMapping("/login")
    public JsonResult login(@RequestBody MsUser user) {
        // 调用服务层方法进行登录
        String token = userService.login(user);
        // 如果 token 为 blocked，说明登录失败
        if(token.equals(USER_BLOCKED))
        {
            return JsonResult.failResult("账号被封禁！");
        }
        // 如果 token 为 null，说明登录失败
        if (token==null) {
            return JsonResult.failResult("用户名或密码错误");
        }
        // 返回成功响应，包含 token
        return JsonResult.result(token);
    }

    @PostMapping("/register")
    public JsonResult register(@RequestBody MsUser msUser, HttpSession session) {
        try {
            userService.saveMsUser(msUser,session);
            return JsonResult.result("成功！");
        }catch (Exception e){
            e.printStackTrace();
            return JsonResult.failResult(e.getMessage());
        }
    }

    @PostMapping("/editUserInfo")
    public JsonResult editUserInfo(@RequestBody MsUser user, HttpServletRequest request) {
        // 从 ThreadLocal 中获取当前用户信息
        MsUserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            return JsonResult.failResult("用户未登录");
        }

        // 设置用户 ID
        user.setId(userDTO.getId());
        user.setDate(StringUtils.getNowDate());

        // 更新用户信息
        boolean success = userService.updateById(user);
        if (success) {
            return JsonResult.result("修改成功");
        } else {
            return JsonResult.failResult("修改失败");
        }
    }


    /**
     * 从token中得到用户信息
     * @return
     */
    @GetMapping ("/userInfo")
    public JsonResult userInfo(HttpServletRequest request)
    {
        // 从 ThreadLocal 中获取当前用户信息（由拦截器设置）
        MsUserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            return JsonResult.failResult("用户未登录");
        }

        // 查询数据库获取完整用户信息
        MsUser user = userService.getById(userDTO.getId());
        if (user == null) {
            return JsonResult.failResult("用户不存在");
        }

        // 返回用户信息
        return JsonResult.result(user);
    }

    /**
     * 用于主页获得用户信息（DTO版）
     * @return
     */
    @GetMapping("/me")
    public JsonResult me() {
        // 从 ThreadLocal 中获取当前用户信息（由拦截器设置）
        MsUserDTO userDTO = UserHolder.getUser();
        // 返回用户信息,没有内容对应游客状态，有内容对应登录状态
        return JsonResult.result(userDTO);
    }

    /**
     * 登出方法
     * @return
     */
    @PostMapping("/logout")
    public JsonResult logout(HttpServletRequest request){
        // 从请求头中获取 token
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            return JsonResult.failResult("未登录");
        }

        // 删除 Redis 中的用户信息
        stringRedisTemplate.delete(LOGIN_USER_KEY + token);
        UserHolder.removeUser();

        return JsonResult.result("退出成功");
    }
}
