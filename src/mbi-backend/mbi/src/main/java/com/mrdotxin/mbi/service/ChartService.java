package com.mrdotxin.mbi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mrdotxin.mbi.model.dto.chart.BIChartResponse;
import com.mrdotxin.mbi.model.dto.chart.ChartQueryRequest;
import com.mrdotxin.mbi.model.dto.chart.GenerateChartRequest;
import com.mrdotxin.mbi.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author MrXin
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2025-04-18 18:49:07
*/
public interface ChartService extends IService<Chart> {

    /**
     * @param chartQueryRequest 查询条件
     * @return QueryWrapper<Chart>
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

    /**
     * 校验上传的文件
     * @param multipartFile
     * @param generateChartRequest
     */
    void validRequest(MultipartFile multipartFile, GenerateChartRequest generateChartRequest);

    /**
     * 更新图表的查看状态, 即用户有没有查看过生成的图表
     * @param chartId
     * @param state
     */
    void changeCheckState(long chartId, boolean state);

    /**
     * 更新
     * @param chartId
     * @param genState
     */
    void changeGenState(long chartId, String genState);

    /**
     *
     * @param chartId
     * @param genResult
     * @param genCode
     */
    void updateGenResultAndState(long chartId, String genResult, String genCode);

    /**
     *
     * @param chart
     * @return
     */
    List<String> buildChartAiPrompt(Chart chart);

    /**
     *
     * @param genResult
     * @return
     */
    BIChartResponse buildResponseWithGenResult(String genResult);
}
