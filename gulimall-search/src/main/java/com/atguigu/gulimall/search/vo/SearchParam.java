package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParam {

    private String keyword; // 全文匹配关键字
    private Long catalog3Id; // 三级分类id

    /**
     * 过滤条件
     * hasStock=0/1
     * skuPrice=1_500/_500/500_
     * brandId=1,若多个品牌则均单独作为一项条件，如brandId=1&brandId=2&...
     * attrs=2_5寸:6寸
     */
    private List<Long> brandId; // 品牌，可以多选
    private List<String> attrs; // 属性，可以多选
    private String skuPrice; // 价格区间
    private Integer hasStock; // 是否有货

    /**
     * 排序条件
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     */
    private String sort;

    /**
     * 分页
     */
    private Integer pageNum = 1; // 页码

    /**
     * 原生所有查询属性
     */
    private String _queryString;
}
