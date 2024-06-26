package com.moye.moyebi.bizmq;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.moye.moyebi.bizmq.BiMqConstant.BI_EXCHANGE_NAME;
import static com.moye.moyebi.bizmq.BiMqConstant.BI_ROUTING_KEY;

import javax.annotation.Resource;

@Component
public class BiMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息的方法
     *
     * @param message
     */
    public void sendMessage(String message) {

        MessageProperties messageProperties = new MessageProperties();
        // 设置超时时间为4分钟（240秒），其中模型处理时间为225s
        messageProperties.setExpiration("240000");
        Message msg = new Message(message.getBytes(), messageProperties);

        rabbitTemplate.convertAndSend(BI_EXCHANGE_NAME, BI_ROUTING_KEY, msg);
    }

}
