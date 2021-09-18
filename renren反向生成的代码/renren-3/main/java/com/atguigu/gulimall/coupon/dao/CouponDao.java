package com.atguigu.gulimall.coupon.dao;

import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author yunuozju
 * @email 2246463432@qq.com
 * @date 2021-08-13 17:39:16
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
