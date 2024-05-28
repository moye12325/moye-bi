package com.moye.moyebi.controller;


import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.moye.moyebi.annotation.AuthCheck;
import com.moye.moyebi.common.BaseResponse;
import com.moye.moyebi.common.DeleteRequest;
import com.moye.moyebi.common.ErrorCode;
import com.moye.moyebi.common.ResultUtils;
import com.moye.moyebi.constant.CommonConstant;
import com.moye.moyebi.constant.UserConstant;
import com.moye.moyebi.exception.BusinessException;
import com.moye.moyebi.exception.ThrowUtils;
import com.moye.moyebi.manager.RedisLimiterManager;
import com.moye.moyebi.model.dto.chart.*;
import com.moye.moyebi.model.entity.Chart;
import com.moye.moyebi.model.entity.User;
import com.moye.moyebi.model.vo.BiResponse;
import com.moye.moyebi.service.ChartService;
import com.moye.moyebi.service.OpenaiService;
import com.moye.moyebi.service.UserService;
import com.moye.moyebi.utils.Csv2String;
import com.moye.moyebi.utils.ExcelUtils;
import com.moye.moyebi.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 帖子接口
 */
@RestController
@RequestMapping("/queue")
@Slf4j
@Profile({"dev","local"})
public class QueueTestController {

    @Resource
    // 自动注入一个线程池的实例
    private ThreadPoolExecutor threadPoolExecutor;

    @GetMapping("/add")
    // 接收一个参数name，然后将任务添加到线程池中
    public void add(String name) {
        // 使用CompletableFuture运行一个异步任务
        CompletableFuture.runAsync(() -> {
            // 打印一条日志信息，包括任务名称和执行线程的名称
            log.info("任务执行中：" + name + "，执行人：" + Thread.currentThread().getName());
            try {
                // 让线程休眠10分钟，模拟长时间运行的任务
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 异步任务在threadPoolExecutor中执行
        }, threadPoolExecutor);
    }

    @GetMapping("/get")
    // 该方法返回线程池的状态信息
    public String get() {
        // 创建一个HashMap存储线程池的状态信息
        Map<String, Object> map = new HashMap<>();
        // 获取线程池的队列长度
        int size = threadPoolExecutor.getQueue().size();
        // 将队列长度放入map中
        map.put("队列长度", size);
        // 获取线程池已接收的任务总数
        long taskCount = threadPoolExecutor.getTaskCount();
        // 将任务总数放入map中
        map.put("任务总数", taskCount);
        // 获取线程池已完成的任务数
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        // 将已完成的任务数放入map中
        map.put("已完成任务数", completedTaskCount);
        // 获取线程池中正在执行任务的线程数
        int activeCount = threadPoolExecutor.getActiveCount();
        // 将正在工作的线程数放入map中
        map.put("正在工作的线程数", activeCount);
        // 将map转换为JSON字符串并返回
        return JSONUtil.toJsonStr(map);
    }
}
