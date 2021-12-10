package com.atguigu.gulimall.member;

import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GulimallMemberApplicationTests {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Test
    public void testx() {
//        MemberLevelEntity defaultLevel = memberLevelDao.getDefaultLevel();
//        System.out.println(defaultLevel.getId());

        MemberLevelEntity id = memberLevelDao.selectOne(new QueryWrapper<MemberLevelEntity>().eq("id", 1));
        System.out.println(id.getId());
    }

    @Test
    void contextLoads() {
    }

}
