package com.moye.moyebi.bizmq;

import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class MyMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息的方法
     *
     * @param exchange
     * @param routing
     * @param message
     */
    public void sendMessage(String exchange, String routing, String message) {
        rabbitTemplate.convertAndSend(exchange, routing, message);
    }

}
