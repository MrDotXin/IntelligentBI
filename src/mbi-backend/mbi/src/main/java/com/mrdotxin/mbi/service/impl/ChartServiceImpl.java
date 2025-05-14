package com.mrdotxin.mbi.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrdotxin.mbi.common.ErrorCode;
import com.mrdotxin.mbi.constant.CommonConstant;
import com.mrdotxin.mbi.exception.BusinessException;
import com.mrdotxin.mbi.exception.ThrowUtils;
import com.mrdotxin.mbi.model.dto.chart.BIChartResponse;
import com.mrdotxin.mbi.model.dto.chart.ChartQueryRequest;
import com.mrdotxin.mbi.model.dto.chart.GenerateChartRequest;
import com.mrdotxin.mbi.model.entity.Chart;
import com.mrdotxin.mbi.model.enums.ChartGenStateEnum;
import com.mrdotxin.mbi.service.ChartService;
import com.mrdotxin.mbi.mapper.ChartMapper;
import com.mrdotxin.mbi.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
* @author MrXin
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2025-04-18 18:49:07
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

    @Value("${max-file-request-size}")
    private long maxFileRequestSize;

    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {

        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();

        Long id = chartQueryRequest.getId();
        String chartName = chartQueryRequest.getChartName();
        Long userId = chartQueryRequest.getUserId();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(Objects.nonNull(id) && id > 0, "id", id);
        queryWrapper.eq(Objects.nonNull(userId) && userId > 0, "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.like(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.like(StringUtils.isNotBlank(chartName), "chartName", chartName);

        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);

        return queryWrapper;
    }

    @Override
    public void validRequest(MultipartFile multipartFile, GenerateChartRequest generateChartRequest) {
        // 文件后缀
       String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!Arrays.asList("xlsx", "excel").contains(fileSuffix),
             ErrorCode.PARAMS_ERROR, "文件类型错误");

       // 文件大小
       long size = multipartFile.getSize();
       ThrowUtils.throwIf(size > maxFileRequestSize * 1024 * 1024,
               ErrorCode.PARAMS_ERROR, "文件大小不能超过 1M");

       String chartName = generateChartRequest.getChartName();
       String goal = generateChartRequest.getGoal();
       String chartType = generateChartRequest.getChartType();

       ThrowUtils.throwIf(StringUtils.isAnyBlank(chartName, goal, chartType), ErrorCode.PARAMS_ERROR);
    }

    @Override
    public void changeCheckState(long chartId, boolean state) {
        UpdateWrapper<Chart> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", chartId);
        updateWrapper.set("isChecked", state);

        ThrowUtils.throwIf(!this.update(updateWrapper), new BusinessException(ErrorCode.PARAMS_ERROR));
    }

    @Override
    public void changeGenState(long chartId, String genState) {
        UpdateWrapper<Chart> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", chartId);
        updateWrapper.set("genState", genState);

        this.update(updateWrapper);
    }

    @Override
    public void updateGenResultAndState(long chartId, String genResult, String genChart) {
        UpdateWrapper<Chart> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", chartId);
        updateWrapper.set("genState", ChartGenStateEnum.COMPLETE.getValue());
        updateWrapper.set("isChecked", false);
        updateWrapper.set("genResult", genResult);
        updateWrapper.set("genChart", genChart);

        this.update(updateWrapper);
    }

    @Override
    public List<String> buildChartAiPrompt(Chart chart) {

        String chartName = chart.getChartName();
        String goal = chart.getGoal();
        String chartData = chart.getChartData();
        String chartType = chart.getChartType();

        return Arrays.asList(
                "data:\"",
                chartData,
                "\",",
                String.format("goal: \"%1$s\",", goal),
                String.format("chartType: \"%1$s\",", chartType),
                String.format("chartName: \"%1$s\"", chartName)
        );
    }

    @Override
    public BIChartResponse buildResponseWithGenResult(String genResult) {

        int codeBegin = genResult.indexOf("```json");
        ThrowUtils.throwIf(codeBegin == -1, ErrorCode.PARAMS_ERROR, "AI 生成错误");

        int codeEnd = genResult.indexOf("```", codeBegin + 3);
        ThrowUtils.throwIf(codeEnd == -1, ErrorCode.PARAMS_ERROR, "AI 生成错误");

        String genResultCode = genResult.substring(codeBegin + 7, codeEnd);
        String genResultAnalysis = genResult.substring(codeEnd + 3);

        BIChartResponse biChartResponse = new BIChartResponse();
        biChartResponse.setChartResult(genResultAnalysis);
        biChartResponse.setChartData(genResultCode);

        return biChartResponse;
    }

}




