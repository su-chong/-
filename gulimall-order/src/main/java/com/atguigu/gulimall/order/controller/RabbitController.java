package com.atguigu.gulimall.order.controller;

import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
public class RabbitController {

    @Autowired
    RabbitTemplate template;

    @GetMapping("/sendmq")
    public String sendMq(@RequestParam(value = "num",defaultValue = "10") Integer num) {
        for (int i = 0; i < num; i++) {
            if(i % 2 == 0) {
                template.convertAndSend("hello-java-exchange", "hello.java", i+"");
            } else {
                OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
                reasonEntity.setId(1L);
                reasonEntity.setCreateTime(new Date());
                reasonEntity.setName("reasonEntityMsg" + i);
                template.convertAndSend("hello-java-exchange", "hello.java", reasonEntity);
            }
        }
        return "ok";
    }

}
