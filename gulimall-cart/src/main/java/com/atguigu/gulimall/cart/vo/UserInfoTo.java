package com.atguigu.gulimall.cart.vo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserInfoTo {

    private Long userId;

    private String userKey;

    /**
     * 是否为temp_user
     */
    private Boolean tempUser = false;
}
