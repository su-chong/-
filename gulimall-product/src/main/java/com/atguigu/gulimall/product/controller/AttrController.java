package com.atguigu.gulimall.product.controller;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.service.AttrGroupService;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.atguigu.gulimall.product.vo.AttroRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;



/**
 * 商品属性
 *
 * @author yunuozju
 * @email 2246463432@qq.com
 * @date 2021-08-13 10:15:10
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    AttrService attrService;

    @Autowired
    AttrGroupService attrGroupService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }

//    @GetMapping("/base/list/{catelogId}")
//    public R baseAttrList(@RequestParam Map<String, Object> params,
//                  @PathVariable("catelogId") Long catelogId){
//        PageUtils page = attrService.queryBaseAttrPage(params,catelogId);
//
//        return R.ok().put("page", page);
//    }

    @GetMapping("/{type}/list/{catelogId}")
    public R baseAttrList(@RequestParam Map<String, Object> params,
                          @PathVariable("catelogId") Long catelogId,
                          @PathVariable("type") String type){
        PageUtils page = attrService.queryBaseAttrPage(params,catelogId,type);

        return R.ok().put("page", page);
}


    /**
     * 信息
     */
//    @RequestMapping("/info/{attrId}")
//    public R info(@PathVariable("attrId") Long attrId){
//		AttrEntity attr = attrService.getById(attrId);
//
//        return R.ok().put("attr", attr);
//    }
    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId){
        AttroRespVo attr = attrService.getAttrInfo(attrId);

        return R.ok().put("attr", attr);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrVo attr){
        attrService.saveAttr(attr);

        return R.ok();
    }



    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVo attr){
        attrService.updateAttr(attr);

        return R.ok();
    }
//    @RequestMapping("/update")
//    public R update(@RequestBody AttrEntity attr){
//		attrService.updateById(attr);
//
//        return R.ok();
//    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}
