package com.moye.moyebi.manager;

import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.exception.SparkException;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import io.github.briqt.spark4j.model.response.SparkTextUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author goblin
 * @Date 2024/3/28 2:16
 * @注释
 */
@Service
@Slf4j
public class AiManager {
    @Resource
    private SparkClient sparkClient;

    public String doChat(String message) {
        // 设置认证信息
        // 消息列表，可以在此列表添加历史对话记录
        List<SparkMessage> messages = new ArrayList<>();
        messages.add(SparkMessage.systemContent("{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照以下指定格式生成内容，内容包含“#####”（此外不要输出任何多余的开头、结尾、注释）\n" +
                "#####\n" +
                "{前端Echarts V5的option配置对象的准确json代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
                "#####\n" +
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}"));
        messages.add(SparkMessage.userContent(message));
// 构造请求
        SparkRequest sparkRequest = SparkRequest.builder()
// 消息列表
                .messages(messages)
// 模型回答的tokens的最大长度,非必传，默认为2048。
// V1.5取值为[1,4096]
// V2.0取值为[1,8192]
// V3.0取值为[1,8192]
                .maxTokens(2048)
// 核采样阈值。用于决定结果随机性,取值越高随机性越强即相同的问题得到的不同答案的可能性越高 非必传,取值为[0,1],默认为0.5
                .temperature(0.4)
// 指定请求版本，默认使用最新3.5版本
                .apiVersion(SparkApiVersion.V3_5)
                .build();
        String result ="";
        String useToken = " ";
        try {
            // 同步调用
            SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
            SparkTextUsage textUsage = chatResponse.getTextUsage();
            result = chatResponse.getContent();
            useToken = "提问tokens：" + textUsage.getPromptTokens()
                    + "，回答tokens：" + textUsage.getCompletionTokens()
                    + "，总消耗tokens：" + textUsage.getTotalTokens();
            log.info(useToken);
        } catch (SparkException e) {
            log.error("Ai调用发生异常了：" + e.getMessage());
        }
        return result;
    }

}
