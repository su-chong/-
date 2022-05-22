package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSubmitVo {
    // 收货地址的id
    private Long addrId;
    // 收货方式
    private Integer payType;
    // 防重令牌
    private String orderToken;
    // 应付总额
    private BigDecimal payPrice;
    // 备注
    private String note;

    // 商品 → 到cart查
    // 用户 → 到session查
    // 优惠 发票
}
