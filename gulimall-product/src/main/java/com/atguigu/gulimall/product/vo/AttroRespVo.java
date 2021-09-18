package com.atguigu.gulimall.product.vo;

import lombok.Data;

@Data
public class AttroRespVo extends AttrVo{
    private String groupName;
    private String catelogName;
    private Long[] catelogPath;
}
