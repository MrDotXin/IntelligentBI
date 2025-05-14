package com.mrdotxin.mbi.ai;

import java.util.List;

public interface AIChartService {

    /**
     * 响应请求，根据传入需求, 构造对应prompt
     */
    String doAIChart(List<String> queries);
}
