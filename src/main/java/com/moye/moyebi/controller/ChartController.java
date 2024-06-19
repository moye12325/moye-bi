package com.moye.moyebi.controller;


import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.*;
import com.moye.moyebi.annotation.AuthCheck;
import com.moye.moyebi.bizmq.BiMessageConsumer;
import com.moye.moyebi.bizmq.BiMessageProducer;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.moye.moyebi.utils.SqlUtils;
import org.springframework.web.multipart.MultipartFile;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 帖子接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private OpenaiService openaiService;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageConsumer biMessageConsumer;

    public static final Integer SYNCHRO_MAX_TOKEN = 340;
    @Autowired
    private BiMessageProducer biMessageProducer;


    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<Chart>> listMyChartByPageVO(@RequestBody ChartQueryRequest chartQueryRequest,
                                                         HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }


    // endregion


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }


    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 智能分析
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/genByAi")
    public String genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                               GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        // 获取输入文件
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isBlank(name) && name.length() > 128, ErrorCode.PARAMS_ERROR, "名称为空或者过长");

        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                "分析需求：\n" +
                "{数据分析的需求或者目标}\n" +
                "原始数据：\n" +
                "{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "【【【【【\n" +
                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
                "【【【【【\n" +
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";

        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");
        System.setProperty("proxySet", "true");

        StringBuilder userInput = new StringBuilder();
        userInput.append("假设你是数据分析师，根据分析目标与数据进行分析。").append("\n");
        userInput.append("分析目标" + goal).append("\n");

        // 读取用户上传的文件 -- 根据图表文件改成csv
        String result = ExcelUtils.excelToCsv(multipartFile);
        // 图表压缩后的数据
        userInput.append("数据：").append(result).append("\n");

        return null;

    }

    /**
     * 智能分析--星火讯飞
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/genBySpark")
    public BaseResponse<BiResponse> genChartBySparkLLM(@RequestPart("file") MultipartFile multipartFile,
                                                       GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        BiResponse result = chartService.genChartBySpark(multipartFile, genChartByAiRequest, loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 文件AI分析  chatgpt
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByGpt35(@RequestPart("file") MultipartFile multipartFile,
                                                    GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验上传
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        User loginUser = userService.getLoginUser(request);

        // 校验文件大小及后缀
        long size = multipartFile.getSize();
        final long TEN_MB = 10 * 1024 * 1024L;
        ThrowUtils.throwIf(size > TEN_MB, ErrorCode.PARAMS_ERROR, "文件大小大于1M");
        String fileName = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(fileName);
        final List<String> validFileSuffix = Arrays.asList("xlsx", "csv", "xls");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByGpt35" + loginUser.getId());

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：\n");

        // 拼接分析目标
        String userGoal = "请帮我合理的分析一下数据";
        if (StringUtils.isNotBlank(goal))
            userGoal = goal;

        // 分析输入加入图表类型
        if (StringUtils.isNotBlank(chartType))
            userGoal += ",请使用" + chartType;
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：\n");

        // 压缩数据
        String userData = "";
        if (Objects.equals(suffix, "csv"))
            userData = Csv2String.MultipartFileToString(multipartFile);
        else
            userData = ExcelUtils.excelToCsv(multipartFile);

        BiResponse biResponse = new BiResponse();

        Chart chart = new Chart();

        // 数据规模校验 gpt3.5分析时长超过30s
        if (userData.length() > SYNCHRO_MAX_TOKEN) {
            biResponse.setGenChart("");
            biResponse.setGenResult("large_size");
            biResponse.setChartId(chart.getId());
            return ResultUtils.success(biResponse);
        }
        userInput.append(userData).append("\n");
        String result = "";
        try {
            // 执行重试逻辑
            result = openaiService.doChatWithRetry(userInput.toString());
        } catch (Exception e) {
            // 如果重试过程中出现异常，返回错误信息
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e + "，AI生成错误");
        }
//        scoreService.deductPoints(loginUser.getId(),1L);
        String[] splits = result.split("【【【【【");

        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        // Echarts代码过滤 "var option ="
        if (genChart.startsWith("var option =")) {
            // 去除 "var option ="
            genChart = genChart.replaceFirst("var\\s+option\\s*=\\s*", "");
        }

        JsonObject chartJson = JsonParser.parseString(genChart).getAsJsonObject();

        // 自动加入图表名称结尾并设置图表名称
        if (StringUtils.isEmpty(name)) {
            String genChartName = String.valueOf(chartJson.getAsJsonObject("title").get("text"));
            genChartName = genChartName.replace("\"", "");
            if (!genChartName.endsWith("图") && !genChartName.endsWith("表") && !genChartName.endsWith("图表"))
                genChartName = genChartName + "图";
            chart.setName(genChartName);
        } else
            chart.setName(name);
        // 自动添加图表类型
        if (StringUtils.isEmpty(chartType)) {
            JsonArray seriesArray = chartJson.getAsJsonArray("series");
            if (seriesArray.size() > 0) {
                JsonObject firstSeries = seriesArray.get(0).getAsJsonObject();
                String typeChart = firstSeries.getAsJsonObject().get("type").getAsString();
                String CnChartType = chartService.getChartTypeToCN(typeChart);
                chart.setChartType(CnChartType);
            }
        } else
            chart.setChartType(chartType);
        // 加入下载按钮
        JsonObject toolbox = new JsonObject();
        toolbox.addProperty("show", true);
        JsonObject saveAsImage = new JsonObject();
        saveAsImage.addProperty("show", true);
        saveAsImage.addProperty("excludeComponents", "['toolbox']");
        saveAsImage.addProperty("pixelRatio", 2);
        JsonObject feature = new JsonObject();
        feature.add("saveAsImage", saveAsImage);
        toolbox.add("feature", feature);
        chartJson.add("toolbox", toolbox);
        chartJson.remove("title");
        String updatedGenChart = chartJson.toString();
        chart.setGoal(userGoal);
        chart.setChartData(userData);
        chart.setGenChart(updatedGenChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setStatus("succeed");
        boolean saveResult = chartService.save(chart);
        if (!saveResult)
            handleChartUpdateError(chart.getId(), "图表信息保存失败");
        biResponse.setGenChart(updatedGenChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }


    /**
     * 文件AI分析  chatgpt 异步分析
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByGpt35Async(@RequestPart("file") MultipartFile multipartFile,
                                                         GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验上传
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        User loginUser = userService.getLoginUser(request);

        // 校验文件大小及后缀
        long size = multipartFile.getSize();
        final long TEN_MB = 10 * 1024 * 1024L;
        ThrowUtils.throwIf(size > TEN_MB, ErrorCode.PARAMS_ERROR, "文件大小大于1M");
        String fileName = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(fileName);
        final List<String> validFileSuffix = Arrays.asList("xlsx", "csv", "xls");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByGpt35" + loginUser.getId());

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：\n");

        // 拼接分析目标
        String userGoal = "请帮我合理的分析一下数据";
        if (StringUtils.isNotBlank(goal))
            userGoal = goal;

        // 分析输入加入图表类型
        if (StringUtils.isNotBlank(chartType))
            userGoal += ",请使用" + chartType;
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：\n");

        // 压缩数据
        String userData = "";
        if (Objects.equals(suffix, "csv"))
            userData = Csv2String.MultipartFileToString(multipartFile);
        else
            userData = ExcelUtils.excelToCsv(multipartFile);

        // 插入数据库
        Chart chart = new Chart();
        chart.setStatus("wait");
        chart.setGoal(userGoal);
        chart.setChartData(userData);
        if (!StringUtils.isEmpty(name))
            chart.setName(name);
        if (!StringUtils.isEmpty(chartType))
            chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        if (!saveResult)
            handleChartUpdateError(chart.getId(), "图表初始数据保存失败");

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());

        // todo 解决异常
        CompletableFuture.runAsync(() -> {
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean update = chartService.updateById(updateChart);
            if (!update) {
                handleChartUpdateError(updateChart.getId(), "图表执行状态保存失败");
                return;
            }
            String result = openaiService.doChat(userInput.toString());
            String[] splits = result.split("【【【【【");
            if (splits.length < 3)
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成错误");
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            // Echarts代码过滤 "var option ="
            if (genChart.startsWith("var option =")) {
                // 去除 "var option ="
                genChart = genChart.replaceFirst("var\\s+option\\s*=\\s*", "");
            }
            Chart updateResult = new Chart();
            updateResult.setId(chart.getId());
            JsonObject chartJson;
            String genChartName;
            String updatedGenChart = "";
            try {
                chartJson = JsonParser.parseString(genChart).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "json代码解析异常");
            }
            // 自动添加图表类型
            if (StringUtils.isEmpty(chart.getName())) {
                JsonArray seriesArray = chartJson.getAsJsonArray("series");
                for (JsonElement i : seriesArray) {
                    String typeChart = i.getAsJsonObject().get("type").getAsString();
                    String CnChartType = chartService.getChartTypeToCN(typeChart);
                    updateResult.setChartType(CnChartType);
                }
            }
            // 自动加入图表名称结尾并设置图表名称
            if (StringUtils.isEmpty(chart.getName())) {
                try {
                    genChartName = String.valueOf(chartJson.getAsJsonObject("title").get("text"));
                } catch (JsonSyntaxException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "json代码不存在title字段");
                }
                genChartName = genChartName.replace("\"", "");
                if (!genChartName.endsWith("图") && !genChartName.endsWith("表") && !genChartName.endsWith("图表"))
                    genChartName = genChartName + "图";
                updateResult.setName(genChartName);
                // 加入下载按钮
                JsonObject toolbox = new JsonObject();
                toolbox.addProperty("show", true);
                JsonObject saveAsImage = new JsonObject();
                saveAsImage.addProperty("show", true);
                saveAsImage.addProperty("excludeComponents", "['toolbox']");
                saveAsImage.addProperty("pixelRatio", 2);
                JsonObject feature = new JsonObject();
                feature.add("saveAsImage", saveAsImage);
                toolbox.add("feature", feature);
                chartJson.add("toolbox", toolbox);
                chartJson.remove("title");
                updatedGenChart = chartJson.toString();
                updateResult.setGenChart(updatedGenChart);
            }else {
                updateResult.setGenChart(genChart);
            }
            updateResult.setGenResult(genResult);

            // TODO:枚举值实现
            updateResult.setStatus("succeed");
            boolean code = chartService.updateById(updateResult);
            if (!code){
                handleChartUpdateError(updateResult.getId(), "图表代码保存失败");
            }

            if (updatedGenChart == null || updatedGenChart.isEmpty()){
                biResponse.setGenChart(updatedGenChart);
            }else {
                biResponse.setGenChart(genChart);
            }

            biResponse.setGenResult(genResult);
        }, threadPoolExecutor);

        return ResultUtils.success(biResponse);
    }

    /**
     * 文件AI分析  chatgpt 异步MQ分析
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByGpt35AsyncMQ(@RequestPart("file") MultipartFile multipartFile,
                                                         GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验上传
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        User loginUser = userService.getLoginUser(request);

        // 校验文件大小及后缀
        long size = multipartFile.getSize();
        final long TEN_MB = 10 * 1024 * 1024L;
        ThrowUtils.throwIf(size > TEN_MB, ErrorCode.PARAMS_ERROR, "文件大小大于1M");
        String fileName = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(fileName);
        final List<String> validFileSuffix = Arrays.asList("xlsx", "csv", "xls");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByGpt35" + loginUser.getId());

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：\n");

        // 拼接分析目标
        String userGoal = "请帮我合理的分析一下数据";
        if (StringUtils.isNotBlank(goal))
            userGoal = goal;

        // 分析输入加入图表类型
        if (StringUtils.isNotBlank(chartType))
            userGoal += ",请使用" + chartType;
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：\n");

        // 压缩数据
        String userData = "";
        if (Objects.equals(suffix, "csv"))
            userData = Csv2String.MultipartFileToString(multipartFile);
        else
            userData = ExcelUtils.excelToCsv(multipartFile);

        // 插入数据库
        Chart chart = new Chart();
        chart.setStatus("wait");
        chart.setGoal(userGoal);
        chart.setChartData(userData);
        if (!StringUtils.isEmpty(name))
            chart.setName(name);
        if (!StringUtils.isEmpty(chartType))
            chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        if (!saveResult)
            handleChartUpdateError(chart.getId(), "图表初始数据保存失败");

        // ------------以上通用代码--------------

        long newChartId = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(newChartId));
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(newChartId);
        return ResultUtils.success(biResponse);

    }


    /**
     * 图表错误状态处理
     *
     * @param chartId
     * @param execMessage
     */
    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus("succeed");
//        updateChart.setExecMessage(execMessage);
        boolean b = chartService.updateById(updateChart);
        if (!b)
            log.error("更新图表失败状态错误" + chartId + ":" + execMessage);
    }


}
