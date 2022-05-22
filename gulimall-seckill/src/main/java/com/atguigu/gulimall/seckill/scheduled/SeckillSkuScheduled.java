package com.atguigu.gulimall.seckill.scheduled;

import com.atguigu.gulimall.seckill.service.SeckillService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SeckillSkuScheduled {

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    SeckillService seckillService;

    private final String upload_lock = "seckill:upload:lock";

    @Scheduled(cron = "*/10 * * 1 * *")
    public void uploadSeckillSkuLatest3Days() {

        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLatest3Days();
        } finally {
            lock.unlock();
        }
    }
}
