package com.moye.moyebi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moye.moyebi.model.entity.Score;


public interface ScoreService extends IService<Score> {
    /**
     * 签到
     * @param userId
     * @return
     */
    void checkIn(Long userId);

    /**
     * 消耗积分
     * @param userId
     * @param points 积分数
     * @return
     */
    void deductPoints(Long userId, Long points);

    /**
     *获取积分
     * @param userId
     * @return
     */
    Long getUserPoints(Long userId);

    /**
     *
     * 获取是否签到状态
     * @param userId
     * @return
     */
    int getIsSign(Long userId);
}
