package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSubmitVo {

    private Long addId;
    // 收货方式
    private Integer payType;
    private String orderToken;
    // 应付总额
    private BigDecimal payPrice;
    // 备注
    private String note;
}
