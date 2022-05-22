package com.atguigu.gulimall.ware.exception;

public class NoStockException extends RuntimeException{

    private Long skuId;

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public NoStockException(Long skuId) {
        super("商品(skuId=" + skuId + ")库存不足");
    }
}
