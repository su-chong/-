package com.atguigu.gulimall.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class GulimallAuthServerApplicationTests {



    @Test
    void contextLoads() {

    }

    @Test
    public void testx() {

    }

    @Autowired
    RedisTemplate<String,String> redisTemplate;

    @Test
    public void testRedis() {
        redisTemplate.opsForValue().set("aa:bb:name", "ming");
    }

}
