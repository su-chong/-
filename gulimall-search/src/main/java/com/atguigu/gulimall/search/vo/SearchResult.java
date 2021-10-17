package com.atguigu.gulimall.search.vo;

import com.atguigu.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResult {

    private List<SkuEsModel> products;

    private List<CatalogVo> catalogs;
    private List<BrandVo> brands;
    private List<AttrVo> attrs;

    private Long total; // 总记录数
    private Integer totalPages; // 总页码
    private Integer pageNum; // 当前页码

    private List<Integer> pageNavs;

    // 面包屑导航数据
    private List<Long> attrIds = new ArrayList<>(); // 便于判断当前id是否被使用
    private List<NavVo> navs = new ArrayList<>();

    //	================以上是返回给页面的所有信息================
    @Data
    public static class NavVo{
        private String name;
        private String navValue;
        private String link;
    }

    @Data
    public static class BrandVo{
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static class CatalogVo{
        private Long catalogId;
        private String catalogName;
    }

    @Data
    public static class AttrVo{
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }
}
