package com.moye.moyebi.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class MultiConsumer {

    // 声明队列名称为"multi_queue"
    private static final String TASK_QUEUE_NAME = "multi_queue";

    public static void main(String[] argv) throws Exception {

        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        // 设置连接工厂主机地址
        factory.setHost("localhost");
        // 从工厂获取新的链接
        final Connection connection = factory.newConnection();

        for (int i = 0; i < 3; i++) {
            // 从链接获取一个新的频道
            final Channel channel = connection.createChannel();
            // 声明一个队列,并设置属性:队列名称,持久化,非排他,非自动删除,其他参数;如果队列不存在,则创建它
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            // (这个先注释)设置预取计数为1，这样RabbitMQ就会在给消费者新消息之前等待先前的消息被确认
            channel.basicQos(1);

            int finalI = i;
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");

                try {
                    System.out.println(" [x] Received '" + "编号：" + finalI + message + "'");
                    Thread.sleep(20000);
//                doWork(message);
                } catch (InterruptedException e) {
                    // 模拟处理消息所花费的时间
                    e.printStackTrace();
                } finally {
                    System.out.println(" [x] Done");
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            // 开始消费消息,传入队列名称,是否自动确认,投递回调和消费者取消回调
            channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> {
            });
        }
    }

//
//    private static void doWork(String task) {
//        for (char ch : task.toCharArray()) {
//            if (ch == '.') {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException _ignored) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//        }
//    }
}