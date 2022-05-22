package com.atguigu.gulimall.seckill.scheduled;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@EnableAsync
@Component
@EnableScheduling
public class HelloSchedule2 {
    /**
     * ① 用CompletableFuture
     * @throws InterruptedException
     */
//    @Scheduled(cron = "* * * * * *")
//    public void hello() throws InterruptedException {
//        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//            System.out.println("线程" + Thread.currentThread().getId() + " => " + new Date());
//            Thread.sleep(3000);
//        }, executor);
//        CompletableFuture.allOf(future);
//    }


    /**
     * ② 改TaskSchedulingAutoConfiguration里的线程数
     * spring.task.scheduling.pool.size=2
     * 结果表明：没用
     *
     * @throws InterruptedException
     */
//    @Scheduled(cron = "* * * * * *")
//    public void hello2() throws InterruptedException {
//        System.out.println("线程" + Thread.currentThread().getId() + " => " + new Date());
//        Thread.sleep(3000);
//    }

    /**
     * ③用异步做
     * "@EnableAsync" + "@Async"
     */
//    @Async
//    @Scheduled(cron = "* * * * * *")
//    public void hello3() throws InterruptedException {
//        System.out.println("线程" + Thread.currentThread().getId() + " => " + new Date());
//        Thread.sleep(3000);
//    }

}
