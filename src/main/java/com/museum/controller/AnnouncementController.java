package com.museum.controller;


import com.museum.config.JsonResult;
import com.museum.domain.po.MsAnnouncement;
import com.museum.domain.query.PageQuery;
import com.museum.service.impl.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器（公告controller）
 * </p>
 *
 * @since 2023-12-19
 */
@RestController
@RequestMapping("/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    @Resource
    AnnouncementService announcementService;

    @PostMapping("/listMsAnnouncement")
    public JsonResult listDicTyp(@RequestBody PageQuery pageQuery) {
        return JsonResult.result(announcementService.listMsAnnouncement(pageQuery));
    }

    @PostMapping("/listMsAnnouncementTop")
    public JsonResult listMsAnnouncementTop(@RequestBody PageQuery pageQuery) {
        try {
            return JsonResult.result(announcementService.listMsAnnouncementTop(pageQuery));
        }catch (Exception e){
            return JsonResult.failResult(e.getMessage());
        }
    }


}
