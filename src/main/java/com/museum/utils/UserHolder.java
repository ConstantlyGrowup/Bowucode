package com.museum.utils;

import com.museum.domain.dto.MsUserDTO;

public class UserHolder {
    private static final ThreadLocal<MsUserDTO> tl = new ThreadLocal<>();

    public static void saveUser(MsUserDTO user){
        tl.set(user);
    }

    public static MsUserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
