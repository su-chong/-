package com.atguigu.gulimall.seckill.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// todo 这个配置就为了用这两个annotatio，能管其他class
@EnableAsync
@EnableScheduling
@Configuration
public class ScheduledConfig {
}
