package com.moye.moyebi.mq;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

public class SingleProducer {
    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv) throws Exception {

        // 创建连接
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // 参数： queue – 队列的名称
            // durable – true 如果我们声明一个持久的队列（队列将在服务器重新启动后继续存在）
            // exclusive – true 如果我们声明一个独占队列（仅限于此连接）
            // autoDelete – true 如果我们声明一个自动删除队列（服务器将在不再使用时将其删除）
            // arguments – 队列的其他属性（构造参数）
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            String message = "Hello World!";

            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            System.out.println(" [x] Sent '" + message + "'");
        }
    }
}