package com.atguigu.gulimall.ware.vo;

import lombok.Data;

@Data
public class LockStockResultVo {

    private Long skuId;

    private Integer num;

    private boolean lock;
}
