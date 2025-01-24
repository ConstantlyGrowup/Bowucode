package com.museum.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.config.ExcepUtil;
import com.museum.config.PageResult;
import com.museum.damain.po.MsAnnouncement;
import com.museum.damain.query.PageQuery;
import com.museum.mapper.MsAnnouncementMapper;
import com.museum.utils.StringUtils;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类（公告管理）
 * </p>
 *
 * @since 2023-12-19
 */
@Service
public class AnnouncementService extends ServiceImpl<MsAnnouncementMapper, MsAnnouncement> implements IService<MsAnnouncement> {

    /**
     * 获取公告信息
     * @return
     */
    public PageResult<MsAnnouncement> listMsAnnouncement(PageQuery pageQuery) {
        Page<MsAnnouncement> pageResult = lambdaQuery().
                like(MsAnnouncement::getTitle, pageQuery.getName()).page(pageQuery.toMpPage());
        return PageResult.of(pageResult, pageResult.getRecords());
    }

    /**
     * 获取置顶公告，根据时间的先后顺序
     * @return
     */
    public PageResult<MsAnnouncement> listMsAnnouncementTop(PageQuery pageQuery) {
        Page<MsAnnouncement> pageResult = lambdaQuery().
                like(MsAnnouncement::getTitle, pageQuery.getName()).eq(MsAnnouncement::getIsTop,"1")
                .orderByDesc(MsAnnouncement::getDate).page(pageQuery.toMpPage());
        return PageResult.of(pageResult, pageResult.getRecords());
    }

    /**
     * 保存公告信息
     * @return
     */
    public void addMsAnnouncement(MsAnnouncement msAnnouncement) throws Exception {
        msAnnouncement.setDate(StringUtils.getNowDateTIme()); // 更新的时候插入当前时间
        save(msAnnouncement);
    }
    /**
     * 删除公告信息，根据ID
     * @return
     */
    public void delMsAnnouncement(Integer id) {
        removeById(id);
    }
    /**
     * 编辑公告信息，例如置顶。Id
     * @return
     */
    public void editMsAnnouncement(MsAnnouncement msAnnouncement) throws Exception {
        if(msAnnouncement.getId() == null) {
            ExcepUtil.throwErr("ID不能未空！");
        }
        msAnnouncement.setDate(StringUtils.getNowDateTIme());
        saveOrUpdate(msAnnouncement);
    }
}
