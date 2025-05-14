package com.mrdotxin.mbi.model.dto.chart;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 *
 *
 */
@Data
public class ChartAddRequest implements Serializable {

    /**
     *
     */
    private Long userId;

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