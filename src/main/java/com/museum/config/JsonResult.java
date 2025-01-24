package com.museum.config;

import lombok.Data;

/**
 * Author: D.Yang
 * Email: koyangslash@gmail.com
 * Date: 16/8/31
 * Time: 下午5:50
 * Describe: 封装Json返回信息
 */
@Data
public class JsonResult {
    public static final Integer CODE_SUCCESS = 200;
    public static final Integer CODE_FAIL = 100;
    public static final String MSG_SUCCESS = "成功";
    public static final String MSG_FAIL = "失败";
    private Integer code;
    private String msg;
    private Object data;
    public JsonResult(Integer code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
    public static JsonResult result(Object data) {
        return  new JsonResult(CODE_SUCCESS, MSG_SUCCESS, data);
    }

    public static JsonResult resultPage(Object data) {
        return  new JsonResult(CODE_SUCCESS, MSG_SUCCESS, data);
    }

    public static JsonResult failResult(String errMsg) {
        return  new JsonResult(CODE_FAIL, errMsg, null);
    }
}
