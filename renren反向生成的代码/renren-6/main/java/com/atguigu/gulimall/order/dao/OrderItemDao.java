package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项信息
 * 
 * @author yunuozju
 * @email 2246463432@qq.com
 * @date 2021-08-13 18:16:57
 */
@Mapper
public interface OrderItemDao extends BaseMapper<OrderItemEntity> {
	
}
