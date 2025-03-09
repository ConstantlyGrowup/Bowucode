package com.museum.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.museum.config.JsonResult;
import com.museum.config.PageResult;
import com.museum.domain.dto.CollectionQuery;
import com.museum.domain.dto.FeedBackQuery;
import com.museum.domain.dto.MsUserDTO;
import com.museum.domain.dto.ReserveQuery;
import com.museum.domain.po.*;
import com.museum.domain.query.PageQuery;
import com.museum.service.impl.*;
import com.museum.utils.StringUtils;
import com.museum.utils.UserHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @since 2023-12-19
 */
@RestController
@RequestMapping("/admin")
@Api(tags = "admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    @Resource
    AdminService adminService;
    @Resource
    AnnouncementService announcementService;
    @Resource
    UserService userService;
    @Resource
    CollectionService collectionService;
    @Resource
    FeedBackService feedBackService;
    @Resource
    ReserveService reserveService;
    @Resource
    ReserveDetailService reserveDetailService;


    @ApiOperation("查询管理员")
    @PostMapping("/login")
    public JsonResult queryAdmin(@RequestBody Admin user) {
        String userName = user.getUsername();
        String pwd = user.getPassword();
        Admin admin = adminService.queryAdmin(userName,pwd);
        if(admin != null) {
            return JsonResult.result(admin);
        }else {
            return JsonResult.failResult("用户名或密码错误！");
        }
    }

    //用户操作
    @PostMapping("/getdata")
    public JsonResult getdata(@RequestBody PageQuery pageQuery) {
        PageResult<MsUser> users = userService.listUserPage(pageQuery);
        return JsonResult.result(users);
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
     * 管理员端的编辑用户信息，需要重写
     * @param user
     * @return
     */
    @PostMapping("/editUserInfo")
    public JsonResult editUserInfo(@RequestBody MsUser user) {
        try {
            //获取用户ID
            Integer id = user.getId();
            if(id == null) {
                throw new Exception("用户ID不允许为空！");
            }
            
            // 调用服务层方法更新用户信息
            userService.updateUser(user);
            return JsonResult.result("更新用户信息成功！");
        } catch (Exception e) {
            return JsonResult.failResult(e.getMessage());
        }
    }

    /**
     * 添加用户
     * @param msUser
     * @param session
     * @return
     */
    @PostMapping("/addUser")
    public JsonResult register(@RequestBody MsUser msUser, HttpSession session) {
        try {
            userService.saveMsUser(msUser,session);
            return JsonResult.result("成功！");
        }catch (Exception e){
            e.printStackTrace();
            return JsonResult.failResult(e.getMessage());
        }
    }

    //以下为藏品页面管理

    /**
     * 获取藏品列表
     * @param pageQuery
     * @return
     */
    @PostMapping("/getdataList")
    public JsonResult getdata(@RequestBody CollectionQuery pageQuery) {
        try {
            PageResult<MsCollection> pageResult = collectionService.listMsCollectionList(pageQuery);
            return JsonResult.result(pageResult);
        } catch (Exception e) {
            log.error("获取藏品信息失败", e);
            return JsonResult.failResult("获取藏品信息失败");
        }
    }

    @PostMapping("/addCol")
    public JsonResult addCol(@RequestBody MsCollection msCollection) {
        try {
            collectionService.addColl(msCollection);
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }

    @PostMapping("/editCollInfo")
    public JsonResult editUserInfo(@RequestBody MsCollection msCollection) {
        try {
            collectionService.editColl(msCollection);
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }
    @PostMapping("/delColl")
    public JsonResult delColl(@RequestBody MsCollection msCollection) {
        try {
            if(msCollection.getId() == null) {
                throw new Exception("ID不允许为空！！！");
            }
            collectionService.delColl(msCollection.getId());
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }

    //下方为公告相关代码
    @PostMapping("/addMsAnnouncement")
    public JsonResult delDic(@RequestBody MsAnnouncement msAnnouncement) {
        try {
            announcementService.addMsAnnouncement(msAnnouncement);
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }

    @PostMapping("/editMsAnnouncement")
    public JsonResult editMsAnnouncement(@RequestBody MsAnnouncement msAnnouncement) {
        try {
            announcementService.editMsAnnouncement(msAnnouncement);
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }

    @PostMapping("/delMsAnnouncement")
    public JsonResult delMsAnnouncement(@RequestBody MsAnnouncement msAnnouncement) {
        try {
            announcementService.delMsAnnouncement(msAnnouncement.getId());
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }

    //以下为留言管理
    @PostMapping("/listAllFeedBack")
    public JsonResult listAllFeedBack(@RequestBody FeedBackQuery pageQuery) {
        try {
            PageResult<FeedBack> data = feedBackService.listAllFeedBack(pageQuery);
            return JsonResult.result(data);
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }
    @PostMapping("/delFeedBack")
    public JsonResult delDic(@RequestBody FeedBack feedBack) {
        try {
            feedBackService.delFeedBack(feedBack.getId());
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }

    @PostMapping("/editFeedBack")
    public JsonResult editFeedBack(@RequestBody FeedBack feedBack) {
        try {
            feedBackService.editFeedBack(feedBack);
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }

    //以下为预约相关接口
    @PostMapping("/listMsReserve")
    public JsonResult listMsReserve(@RequestBody ReserveQuery pageQuery) {
        PageResult<MsReserve> result = reserveService.listMsReserve(pageQuery);
        return JsonResult.result(result);
    }
    @PostMapping("/addMsReserve")
    public JsonResult addMsReserve(@RequestBody MsReserve msReserve) {
        try {
            reserveService.addMsReserve(msReserve);
            return JsonResult.result("成功！");
        }catch (Exception e){
            e.printStackTrace();
            return JsonResult.failResult(e.getMessage());
        }
    }

    @PostMapping("/editMsReserve")
    public JsonResult editMsReserve(@RequestBody MsReserve msReserve) {
        try {
            reserveService.editMsReserve(msReserve);
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }

    @PostMapping("/delMsReserve")
    public JsonResult delMsReserve(@RequestBody MsReserve msReserve) {
        try {
            reserveService.delMsReserve(msReserve.getId());
            return JsonResult.result("成功！");
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }

    //以下为预约细节的业务
    @PostMapping("/listDetailReserve")
    public JsonResult listDetailReserve(@RequestBody ReserveQuery pageQuery) {
        PageResult<MsReserveDetail> result = reserveDetailService.listMsReserveDetail(pageQuery);
        return JsonResult.result(result);
    }
    @PostMapping("/editDetail")
    public JsonResult editDetail(@RequestBody MsReserveDetail detial) {
        try {
            reserveDetailService.editDetail(detial);
            return JsonResult.result("成功！");
        }catch (Exception e){
            e.printStackTrace();
            return JsonResult.failResult(e.getMessage());
        }
    }
    @PostMapping("/delMsReserveDetail")
    public JsonResult delDetail(@RequestBody MsReserveDetail msReserve) {
        try {
            reserveDetailService.delDetail(msReserve.getId());
            return JsonResult.result("成功！");
        }catch (Exception e){
            e.printStackTrace();
            return JsonResult.failResult(e.getMessage());
        }
    }
}
