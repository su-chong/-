package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性&属性分组关联
 * 
 * @author yunuozju
 * @email 2246463432@qq.com
 * @date 2021-08-12 17:48:52
 */
@Mapper
public interface AttrAttrgroupRelationDao extends BaseMapper<AttrAttrgroupRelationEntity> {

    void batchDeleteRelation(@Param("entities") List<AttrAttrgroupRelationEntity> entities);
}
