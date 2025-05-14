package com.mrdotxin.mbi.model.dto.chart;

import com.mrdotxin.mbi.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 *
 *
 */
@Data
public class  ChartQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     *
     */
    private String chartName;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;



    private static final long serialVersionUID = 1L;
}