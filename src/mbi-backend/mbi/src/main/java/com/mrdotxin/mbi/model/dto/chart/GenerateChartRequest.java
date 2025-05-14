package com.mrdotxin.mbi.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenerateChartRequest implements Serializable {

    /**
     *
     */
    private String chartName;

    /**
     *
     */
    private String goal;

    /**
     *
     */
    private String chartType;

    public static final Long seralVersionUID = 1L;
}
