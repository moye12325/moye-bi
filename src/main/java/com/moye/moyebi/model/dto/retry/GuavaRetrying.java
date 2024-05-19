package com.moye.moyebi.model.dto.retry;

import com.github.rholder.retry.*;

import com.google.common.base.Predicates;
import com.moye.moyebi.manager.AiManager;
import com.moye.moyebi.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class GuavaRetrying {
    @Resource
    private AiManager aiManager;

    public String retryDoChart(String userInput){

        String aiResult = null;

        Callable<String> callable = () -> {
            // 业务逻辑
            return aiManager.doChat(userInput);
//            return "123";
        };

        // 定义重试器
        Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
                .retryIfResult(Predicates.<String>isNull()) // 如果结果为空则重试
                .retryIfResult(result ->{
                    String[] splits = result.split("#####");
                    if (splits.length < 3) {
                        return true;
                    }
                    String genChart = splits[1].trim();
                    if(StringUtils.isValidStrictly(genChart)){
                        // 格式正确，不重试
                        return false;
                    }
                    return true;
                })
                .retryIfExceptionOfType(IOException.class) // 发生IO异常则重试
                .retryIfRuntimeException() // 发生运行时异常则重试
                // 初始等待时间，即第一次重试和第二次重试之间的等待时间为 3 秒。
                // 等待时间的增量，意味着每次重试后的等待时间会在前一次的基础上增加 2 秒。
                .withWaitStrategy(WaitStrategies.incrementingWait(3, TimeUnit.SECONDS, 2, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)) // 允许执行3次（首次执行 + 最多重试2次）
                .withRetryListener(new MyRetryListener()) // 添加自定义的重试监听器
                .build();

        try {
            aiResult = retryer.call(callable);// 执行
        } catch (RetryException | ExecutionException e) { // 重试次数超过阈值或被强制中断
            e.printStackTrace();
        }
        return aiResult;
    }
}
