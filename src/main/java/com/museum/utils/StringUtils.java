package com.museum.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StringUtils extends org.apache.commons.lang3.StringUtils {
    public String getBlankStr(Object obj) {
        if(obj == null) {
            return "";
        }
        return obj.toString();
    }

    public static String getNowDateTIme() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return sdf.format(new Date());
    }

    public static String getNowDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        return sdf.format(new Date());
    }
}