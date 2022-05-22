package com.atguigu.gulimall.order.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.order.dao.OrderReturnReasonDao;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import com.atguigu.gulimall.order.service.OrderReturnReasonService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;


@RabbitListener(queues = {"hello-java-queue"})
@Service("orderReturnReasonService")
public class OrderReturnReasonServiceImpl extends ServiceImpl<OrderReturnReasonDao, OrderReturnReasonEntity> implements OrderReturnReasonService {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderReturnReasonEntity> page = this.page(
                new Query<OrderReturnReasonEntity>().getPage(params),
                new QueryWrapper<OrderReturnReasonEntity>()
        );

        return new PageUtils(page);
    }

//    public void receiveMessage(Message message, OrderReturnReasonEntity reasonEntity, Channel channel) {
//        System.out.println("接收到:"+ reasonEntity);
//    }

//    @RabbitHandler
    public void receiveStringMessage(String s) {
        System.out.println("String接收点收到:"+ s);
    }

//    @RabbitHandler
    public void receiveEntityMessage(OrderReturnReasonEntity entity) {
        System.out.println("Entity接收点收到:"+ entity);
    }

    @RabbitHandler
    public void receiveStringMessage2(Message message,String s, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            // 拒签message
            // 开启manual签收后，不写签收语句则默认拒绝
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("String接收点收到:"+ s);
    }

    @RabbitHandler
    public void receiveEntityMessage2(Message message,OrderReturnReasonEntity entity,Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 签收message
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Entity接收点收到:"+ entity);
    }

}