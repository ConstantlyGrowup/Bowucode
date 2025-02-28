package com.museum.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    LocalDateTime ExpireTime;
    Object data;
}
