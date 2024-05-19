package com.moye.moyebi.service;

import com.moye.moyebi.model.dto.chart.GenChartByAiRequest;
import com.moye.moyebi.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moye.moyebi.model.entity.User;
import com.moye.moyebi.model.vo.BiResponse;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 19423
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2024-05-16 15:19:18
*/
public interface ChartService extends IService<Chart> {

    BiResponse genChartBySpark(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser);

    /**
     * 校验文件是否合规
     *
     * @param multipartFile
     */
    void validFile(MultipartFile multipartFile);

    /**
     * 构建用户输入
     *
     * @param goal
     * @param csvData
     * @param chartType
     * @return
     */
    String buildUserInput(String goal, String csvData, String chartType);

    String getChartTypeToCN(String typeChart);
}
