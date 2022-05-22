package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author yunuozju
 * @email 2246463432@qq.com
 * @date 2021-08-13 10:15:10
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
