package com.moye.moyebi.config;

import io.github.briqt.spark4j.SparkClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "spring.spark")
@Data
public class SparkClientConfig {
    private String appid;
    private String apiKey;
    private String apiSecret;

    @Bean
    public SparkClient sparkClient(){
        SparkClient sparkClient = new SparkClient();
        sparkClient.appid = appid;
        sparkClient.apiKey = apiKey;
        sparkClient.apiSecret = apiSecret;
        System.out.println("sparkClient = " + sparkClient);
        System.out.println("appid = " + appid);
        System.out.println("apiKey = " + apiKey);
        return sparkClient;
    }

}
