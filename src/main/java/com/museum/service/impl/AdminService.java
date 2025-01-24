package com.museum.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.damain.po.Admin;
import com.museum.mapper.AdminMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 * @since 2023-22-29
 */
@Service
public class AdminService extends ServiceImpl<AdminMapper, Admin> implements IService<Admin> {

    public Admin queryAdmin(String userName,String pwd) {
        List<Admin> admins = lambdaQuery().eq(Admin::getUsername, userName).eq(Admin::getPassword, pwd).list();
        if(admins.isEmpty()) {
            return null;
        }
        return admins.get(0);
    }
}
