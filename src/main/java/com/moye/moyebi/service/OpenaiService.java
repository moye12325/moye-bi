package com.moye.moyebi.service;

/**
 * @description openaiApi接口类
 */
public interface OpenaiService {

    /**
     * AI 对话
     * @param prompt
     * @return
     */
    String doChat(String prompt);

    /**
     * AI 对话错误重试
     * @param userPrompt
     * @return
     * @throws Exception
     */
    String doChatWithRetry(String userPrompt) throws Exception;
}
