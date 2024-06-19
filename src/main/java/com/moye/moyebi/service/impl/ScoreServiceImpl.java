package com.moye.moyebi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moye.moyebi.common.ErrorCode;
import com.moye.moyebi.exception.ThrowUtils;
import com.moye.moyebi.mapper.ScoreMapper;
import com.moye.moyebi.model.entity.Score;
import com.moye.moyebi.service.ScoreService;
import org.springframework.stereotype.Service;

@Service
public class ScoreServiceImpl extends ServiceImpl<ScoreMapper, Score>
    implements ScoreService {

    @Override
    public void checkIn(Long userId) {
        QueryWrapper<Score> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        Score score = this.getOne(queryWrapper);
        ThrowUtils.throwIf(score == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(score.getIsSign()==1,ErrorCode.PARAMS_ERROR,"领取失败，今日已领取");
        Long scoreTotal = score.getScoreTotal();
        UpdateWrapper<Score> updateWrapper = new UpdateWrapper();
        updateWrapper
                //此处暂时写死签到积分
                .eq("userId",userId)
                .set("scoreTotal",scoreTotal+1)
                .set("isSign",1);
        boolean r = this.update(updateWrapper);
        ThrowUtils.throwIf(!r, ErrorCode.OPERATION_ERROR,"更新签到数据失败");
    }

    @Override
    public void deductPoints(Long userId, Long points) {
        QueryWrapper<Score> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        Score score = this.getOne(queryWrapper);
        ThrowUtils.throwIf(score.getScoreTotal()<points,ErrorCode.OPERATION_ERROR,"积分不足，请联系管理员！");
        Long scoreTotal = score.getScoreTotal();
        UpdateWrapper<Score> updateWrapper = new UpdateWrapper();
        updateWrapper
                .eq("userId",userId)
                .set("scoreTotal",scoreTotal-points);
        boolean r = this.update(updateWrapper);
        ThrowUtils.throwIf(!r, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Long getUserPoints(Long userId) {
        QueryWrapper<Score> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        Score score = this.getOne(queryWrapper);
        return score.getScoreTotal();
    }

    @Override
    public int getIsSign(Long userId){
        QueryWrapper<Score> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        Score score = this.getOne(queryWrapper);
        return score.getIsSign();
    }
}




