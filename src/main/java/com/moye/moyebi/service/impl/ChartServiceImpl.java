package com.moye.moyebi.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moye.moyebi.common.ErrorCode;
import com.moye.moyebi.exception.BusinessException;
import com.moye.moyebi.exception.ThrowUtils;
import com.moye.moyebi.model.dto.chart.GenChartByAiRequest;
import com.moye.moyebi.model.dto.retry.GuavaRetrying;
import com.moye.moyebi.model.entity.Chart;
import com.moye.moyebi.model.entity.User;
import com.moye.moyebi.model.enums.ChartStatusEnum;
import com.moye.moyebi.model.vo.BiResponse;
import com.moye.moyebi.service.ChartService;
import com.moye.moyebi.mapper.ChartMapper;
import com.moye.moyebi.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
* @author 19423
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-05-16 15:19:18
*/
@Service
@Slf4j
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

    @Resource
    private GuavaRetrying guavaRetrying;

    private static final long ONE_MB = 1024 * 1024L;

    @Override
    public BiResponse genChartBySpark(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) {
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();

        // 校验数据
        // 分析目标不为空
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        // 如果名称存在且大于100 名称过长
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 校验文件
        this.validFile(multipartFile);

        // 限流
//        redisLimiterManager.doRateLimit("genChartByAi+" + loginUser.getId());

        String csvData = ExcelUtils.excelToCsv(multipartFile);
        String userInput = this.buildUserInput(goal, csvData, chartType);

        // 拿到已校验过的结果
//        String result = this.validAiResult(userInput);
//         三次重试机会
        String result = guavaRetrying.retryDoChart(userInput);
        if(result == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        String[] splits = result.split("#####");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();


        // 将结果插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setStatus(ChartStatusEnum.SUCCEED.getValue());
        boolean saveResult = this.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        // 封装返回结果
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        log.info("{}使用了AI分析功能", loginUser.getId());
        return biResponse;
    }

    /**
     * 构建用户输入
     *
     * @param goal
     * @param csvData
     * @param chartType
     * @return
     */
    @Override
    public String buildUserInput(String goal, String csvData, String chartType) {

        // 实现分析
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal);
        userInput.append("数据：").append("\n");
        // 压缩后的数据（把multipartFile传进来，其他的东西先注释）
        userInput.append(csvData);

        return userInput.toString();
    }

    @Override
    public String getChartTypeToCN(String typeChart) {
        switch (typeChart){
            case "line":
                return "折线图";
            case "bar":
                return "柱状图";
            case "pie":
                return "饼图";
            case "scatter":
                return "散点图";
            case "radar":
                return "雷达图";
            case "map":
                return "地图";
            case "candlestick":
                return "K线图";
            case "heatmap":
                return "热力图";
            case "tree":
                return "树图";
            case "lines":
                return "路线图";
            case "graph":
                return "关系图";
            case "sunburst":
                return "旭日图";
            default:
                return "特殊图表";
        }
    }

    /**
     * 校验文件是否合规
     * @param multipartFile
     */
    @Override
    public void validFile(MultipartFile multipartFile) {
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        // 文件大小

        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件内容过大");
        // 后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        List<String> validFileSuffix = Arrays.asList("xlsx", "xls", "csv");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式非法");
    }
}




