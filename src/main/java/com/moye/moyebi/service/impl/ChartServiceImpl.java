package com.moye.moyebi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moye.moyebi.model.entity.Chart;
import com.moye.moyebi.service.ChartService;
import com.moye.moyebi.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author 19423
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-05-16 15:19:18
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}




