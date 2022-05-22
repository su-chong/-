package com.atguigu.gulimall.seckill.scheduled;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class HelloSchedule {

    /**
     * cron表达式共有6位，不支持写year
     * 其星期位里，1表示monday，2表示tuesday，依次类推
     */
    @Scheduled(cron = "* * * * * *")
    public void hello() {
        System.out.println("hello");
    }

}
