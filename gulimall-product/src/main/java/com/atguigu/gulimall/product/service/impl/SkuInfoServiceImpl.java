package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.feign.SeckillFeignService;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.ItemSaleAttrVo;
import com.atguigu.gulimall.product.vo.SeckillInfoVo;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SeckillFeignService seckillFeignService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    AttrGroupService attrGroupService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;
//    @Autowired
//    ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> queryWrapper = new QueryWrapper<>();
        /**
         * key:
         * catelogId: 0
         * brandId: 0
         * min: 0
         * max: 0
         */
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            queryWrapper.and((wrapper)->{
                wrapper.eq("sku_id",key).or().like("sku_name",key);
            });
        }

        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId)&&!"0".equalsIgnoreCase(catelogId)){

            queryWrapper.eq("catalog_id",catelogId);
        }

        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId)&&!"0".equalsIgnoreCase(catelogId)){
            queryWrapper.eq("brand_id",brandId);
        }

        String min = (String) params.get("min");
        if(!StringUtils.isEmpty(min)){
            queryWrapper.ge("price",min);
        }

        String max = (String) params.get("max");

        if(!StringUtils.isEmpty(max)  ){
            try{
                BigDecimal bigDecimal = new BigDecimal(max);

                if(bigDecimal.compareTo(new BigDecimal("0"))==1){
                    queryWrapper.le("price",max);
                }
            }catch (Exception e){

            }
        }

        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 利用spuId，返回List<skuInfoEntity>
     * @param spuId
     * @return
     */
    @Override
    public List<SkuInfoEntity> getSkuIdsBySpuId(Long spuId) {
        List<SkuInfoEntity> list = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return list;
    }

    /**
     * 返回商品详情
     * @param skuId
     * @return
     */
    @Override
    public SkuItemVo item(Long skuId) {
        ExecutorService executor = Executors.newFixedThreadPool(10);

        SkuItemVo skuItemVo = new SkuItemVo();

        // sku的基本信息 → sku_info表
//        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
//            SkuInfoEntity info = getById(skuId);
//            skuItemVo.setInfo(info);
//            return info;
//        }, executor);

        SkuInfoEntity info = getById(skuId);
        skuItemVo.setInfo(info);

        // sku的图片 → sku_images表
//        CompletableFuture<Void> ImagesFuture = CompletableFuture.runAsync(() -> {
//            List<SkuImagesEntity> images = skuImagesService.getImagesBySkuId(skuId);
//            skuItemVo.setImages(images);
//        }, executor);
        List<SkuImagesEntity> images = skuImagesService.getImagesBySkuId(skuId);
        skuItemVo.setImages(images);

        // spu的详情图 → spu_info_desc表
//        infoFuture.thenAcceptAsync(res -> {
//            SpuInfoDescEntity byId = spuInfoDescService.getById(res.getSpuId());
//            skuItemVo.setDesc(byId);
//        }, executor);
        SpuInfoDescEntity byId = spuInfoDescService.getById(info.getSpuId());
        skuItemVo.setDesc(byId);

        // spu的属性 → 多张表联合查询：attr_group表, attr_attrgroup_relation表, attr表, product_attr_value表
//        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync(res -> {
//            List<SpuItemAttrGroupVo> attrGroups = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
//            skuItemVo.setGroupAttrs(attrGroups);
//        }, executor);
        List<SpuItemAttrGroupVo> attrGroups = attrGroupService.getAttrGroupWithAttrsBySpuId(info.getSpuId(), info.getCatalogId());
        skuItemVo.setGroupAttrs(attrGroups);

        // spu的销售属性 →
//        CompletableFuture<Void> saleAttrFuture =infoFuture.thenAcceptAsync(res -> {
//            List<ItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrsBuSpuId(res.getSpuId());
//            skuItemVo.setSaleAttr(saleAttrVos);
//        },executor);
        List<ItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrsBuSpuId(info.getSpuId());
        skuItemVo.setSaleAttr(saleAttrVos);

        // 秒杀信息
        R r = seckillFeignService.getSkuSeckillInfo(skuId);
        if(r.getCode() == 0) {
            SeckillInfoVo seckillInfoVo = r.getData(new TypeReference<SeckillInfoVo>() {});
            skuItemVo.setSeckillInfoVo(seckillInfoVo);
        }

        return skuItemVo;
    }

}