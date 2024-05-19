package com.moye.moyebi.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 表格状态枚举
 *
 */
public enum ChartStatusEnum {
    // (排队中0-wait、执行中1-running、已完成2-succeed、3-失败failed)

    WAIT(0, "wait"),
    RUNNING(1, "running"),
    SUCCEED(2, "succeed"),
    FAILED(3, "failed");

    private final String text;

    private final Integer value;

    ChartStatusEnum(Integer value , String text) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static ChartStatusEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (ChartStatusEnum anEnum : ChartStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
