package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author yunuozju
 * @email 2246463432@qq.com
 * @date 2021-08-13 18:06:39
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
