package com.atguigu.gulimall.search.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    @RequestMapping("product/attr/info/{attrId}")
    R attrInfo(@PathVariable("attrId") Long attrId);

    @RequestMapping("product/brand/infos")
    R brandsInfo(@RequestParam("brandIds") List<Long> brandIds);
}
