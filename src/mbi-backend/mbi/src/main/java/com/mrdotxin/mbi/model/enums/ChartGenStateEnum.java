package com.mrdotxin.mbi.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChartGenStateEnum {

    WAITING("等待排队", "waiting"),
    GENERATING("生成中", "generating"),
    COMPLETE("完成", "complete"),
    ERROR("错误", "error");

    private final String text;
    private final String value;


}
