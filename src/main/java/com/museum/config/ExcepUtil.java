package com.museum.config;

public class ExcepUtil {
    public static Exception throwErr(String errInf) throws Exception {
        throw new Exception(errInf);
    }
}
