package com.atguigu.gulimall.order.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderConfirmVo {

    // 所有收货地址
    @Getter @Setter
    List<MemberAddressVo> address;

    // 所有CartItem
    @Getter @Setter
    List<OrderItemVo> items;

    // coupon
    @Getter @Setter
    Integer integration;

    // 总价
    // BigDecimal total;

    // 应付价格
    // BigDecimal payPrice;

    // 防重令牌
    @Getter @Setter
    String orderToken;

    @Getter @Setter
    Map<Long,Boolean> stocks;

    public Integer getCount() {
        Integer count = 0;
        if(items != null && items.size() > 0) {
            for(OrderItemVo item : items) {
                count += item.getCount();
            }
        }
        return count ;
    }


    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if(items != null && items.size() > 0) {
            for(OrderItemVo item : items) {
                BigDecimal total = item.getPrice().multiply(new BigDecimal(item.getCount()));
                sum = sum.add(total);
            }
        }
        return sum;
    }

    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
