package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {

    // sku的基本信息 → sku_info表
    SkuInfoEntity info;

    // sku的图片 → sku_images表
    List<SkuImagesEntity> images;

    // 按sku的销售属性统计
    List<ItemSaleAttrVo> saleAttr;

    // spu的详情图 → spu_info_desc表
    SpuInfoDescEntity desc;

    // spu的属性 → product_attr_value表
    List<SpuItemAttrGroupVo> groupAttrs;

    boolean hasStock = true;

    SeckillInfoVo seckillInfoVo;

}
