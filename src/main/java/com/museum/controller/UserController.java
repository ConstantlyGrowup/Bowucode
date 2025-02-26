package com.museum.controller;


import com.museum.config.JsonResult;
import com.museum.config.PageResult;
import com.museum.domain.dto.MsUserDTO;
import com.museum.domain.po.MsUser;
import com.museum.domain.query.PageQuery;
import com.museum.service.impl.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

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

    @PostMapping("/getdata")
    public JsonResult getdata(@RequestBody PageQuery pageQuery) {
        PageResult<MsUser> users = userService.listUserPage(pageQuery);
        return JsonResult.result(users);
    }

    @PostMapping("/login")
    public JsonResult login(@RequestBody MsUser user, HttpSession session) {
        MsUserDTO dbUser = userService.login(user,session);
        if(dbUser == null) {
            return JsonResult.failResult("用户名密码错误!！");
        }
        if(dbUser.getState() == 1) {
            return JsonResult.failResult("该账号由于违反系统发言规定，已被暂时冻结！请于首页界面找到公告，联系官方申诉");
        }
        return  JsonResult.result(dbUser);
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
    public JsonResult editUserInfo(@RequestBody MsUser msUser) {
        try {
            userService.editUserInfo(msUser);
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }
    @PostMapping("/deluser")
    public JsonResult deluser(@RequestBody MsUser msUser) {
        try {
            if(msUser.getId() == null) {
                throw new Exception("ID不允许为空！！！");
            }
            userService.deluser(msUser.getId());
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }

    /**
     * 收到用户id，返回所有信息到“我的”
     * @param userId
     * @return
     */
    @GetMapping ("/userInfo/{id}")
    public JsonResult userInfo(@PathVariable("id") Integer userId)
    {
        try{
            MsUser userInfo = userService.getuser(userId);
            if(userInfo==null)
            {
                throw new Exception("该用户id不存在！");
            }else {
                return JsonResult.result(userInfo);
            }
        } catch (Exception e) {
            return JsonResult.failResult(e.getMessage());
        }
    }
}
