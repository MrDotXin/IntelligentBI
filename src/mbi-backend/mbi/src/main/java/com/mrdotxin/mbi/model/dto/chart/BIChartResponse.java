package com.mrdotxin.mbi.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/*
 * 此结果用来提供给在生成页面同步等待后的结果
 */
@Data
public class BIChartResponse implements Serializable {

    private String chartData;

    private String chartResult;
}
