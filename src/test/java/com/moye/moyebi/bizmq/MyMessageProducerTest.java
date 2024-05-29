package com.moye.moyebi.bizmq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

// 使用 @SpringBootTest 注解标记该类为一个 Spring Boot 的测试类
@SpringBootTest
class MyMessageProducerTest {

    @Resource
    private MyMessageProducer myMessageProducer;

    @Test
    void sendMessage() {
        myMessageProducer.sendMessage("code_exchange","my_routingKey","哈哈哈哈你好");
    }
}