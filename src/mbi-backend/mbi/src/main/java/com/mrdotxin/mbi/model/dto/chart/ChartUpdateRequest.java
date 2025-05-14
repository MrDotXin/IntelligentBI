package com.mrdotxin.mbi.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新请求
 *

 */
@Data
public class ChartUpdateRequest implements Serializable {
    /**
     * id
     */
    private Long id;
    /**
     *
     */
    private String chartName;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}