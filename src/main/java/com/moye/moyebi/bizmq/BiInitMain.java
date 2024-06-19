package com.moye.moyebi.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.moye.moyebi.bizmq.BiMqConstant.*;

/**
 * 用于创建测试程序用到的交换机和队列（只用在程序启动前执行一次）
 */
public class BiInitMain {

    @SneakyThrows
    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // 声明死信队列及交换机
            channel.exchangeDeclare(DEAD_EXCHANGE, "direct");
            channel.queueDeclare(DEAD_QUEUE_NAME, true, false, false, null);
            channel.queueBind(DEAD_QUEUE_NAME, DEAD_EXCHANGE, DEAD_ROUTING_KEY);

            // 声明正常队列添加死信队列相关设置
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("x-dead-letter-exchange", DEAD_EXCHANGE);
            arguments.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);

            // 声明正常队列及交换机
            channel.queueDeclare(BI_QUEUE_NAME, true, false, false, arguments);
            channel.exchangeDeclare(BI_EXCHANGE_NAME, "direct");
            channel.queueBind(BI_QUEUE_NAME, BI_EXCHANGE_NAME, BI_ROUTING_KEY);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}