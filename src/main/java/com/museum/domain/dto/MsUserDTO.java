package com.museum.domain.dto;

import lombok.Data;

@Data
public class MsUserDTO {
    private Integer id;
    private String username;
    private String nickname;
    private Integer state;
}
