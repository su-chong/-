package com.atguigu.gulimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

public class Cart {
    List<CartItem> items;

    private Integer countNum; // 商品总件数

    private Integer countType; // 商品类型的数量

    private BigDecimal totalAmount; // 商品总价

    private BigDecimal reduce = new BigDecimal("0");

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int count = 0;
        if(items != null && items.size() > 0) {
            for(CartItem item : items) {
                count += item.getCount();
            }
        }
        return count;
    }


    public Integer getCountType() {
        int count = 0;
        if(items != null && items.size() > 0) {
            for(CartItem item : items) {
                count += 1;
            }
        }
        return count;
    }


    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal("0");

        // 计算总价
        if(items != null && items.size() > 0) {
            for(CartItem item : items) {
                if(item.getCheck()) {
                    amount = amount.add(item.getTotalPrice());
                }
            }
        }

        // 减去优惠
        amount.subtract(this.reduce);

        return amount;
    }


    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
