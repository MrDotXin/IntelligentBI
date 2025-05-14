package com.mrdotxin.mbi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrdotxin.mbi.annotation.AuthCheck;
import com.mrdotxin.mbi.bizmq.MBIMQProducer;
import com.mrdotxin.mbi.common.BaseResponse;
import com.mrdotxin.mbi.common.DeleteRequest;
import com.mrdotxin.mbi.common.ErrorCode;
import com.mrdotxin.mbi.common.ResultUtils;
import com.mrdotxin.mbi.constant.UserConstant;
import com.mrdotxin.mbi.exception.BusinessException;
import com.mrdotxin.mbi.exception.ThrowUtils;
import com.mrdotxin.mbi.manager.RedisLimitManager;
import com.mrdotxin.mbi.model.dto.chart.*;
import com.mrdotxin.mbi.model.entity.Chart;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.mrdotxin.mbi.model.entity.User;
import com.mrdotxin.mbi.model.enums.ChartGenStateEnum;
import com.mrdotxin.mbi.ai.AIChartService;
import com.mrdotxin.mbi.service.ChartService;
import com.mrdotxin.mbi.service.UserService;
import com.mrdotxin.mbi.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AIChartService aiChartService;

    @Resource
    private MBIMQProducer mbimqProducer;

    @Resource
    private RedisLimitManager redisLimitManager;

    @Value("${AI.example}")
    private String resultExample;

    @Value("${MQ.MBIExchange}")
    private String mbiExchange;

    @Value("${MQ.ChartGenKey}")
    private String chartGenKey;

    /**
     * 创建
     *
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);

        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);

        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();

        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(Objects.isNull(user), ErrorCode.NOT_LOGIN_ERROR);
        // 非用户自身图表仅管理员
        ThrowUtils.throwIf(userService.isAdmin(user), ErrorCode.NO_AUTH_ERROR);

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());

        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }
    
     @PostMapping("/upload")
    public BaseResponse<BIChartResponse> uploadChartRequest(@RequestPart("file") MultipartFile multipartFile,
                                                   GenerateChartRequest generateChartRequest, HttpServletRequest request) {
         chartService.validRequest(multipartFile, generateChartRequest);

         User loginUser = userService.getLoginUser(request);
         // 针对单个用户的限流
         redisLimitManager.doRateLimit("uploadChartRequest " + loginUser.getId());
         String compressedData = ExcelUtils.convertToCSV(multipartFile);

         Chart chart = new Chart();

         String chartName = generateChartRequest.getChartName();
         String chartType = generateChartRequest.getChartType();
         String goal = generateChartRequest.getGoal();
         chart.setUserId(loginUser.getId());
         chart.setChartName(chartName);
         chart.setGoal(goal);
         chart.setChartData(compressedData);
         chart.setChartType(chartType);
         chartService.save(chart);

         String outcome = aiChartService.doAIChart(chartService.buildChartAiPrompt(chart));
         BIChartResponse biChartResponse = chartService.buildResponseWithGenResult(outcome);
         chartService.updateGenResultAndState(chart.getId(), biChartResponse.getChartResult(), biChartResponse.getChartData());

         return ResultUtils.success(biChartResponse);
    }


    @PostMapping("/upload/async")
    public BaseResponse<Long> uploadChartRequestAsync(@RequestPart("file") MultipartFile multipartFile,
                                                   GenerateChartRequest generateChartRequest, HttpServletRequest request) {
         chartService.validRequest(multipartFile, generateChartRequest);
         User loginUser = userService.getLoginUser(request);
         // 针对单个用户的限流
         redisLimitManager.doRateLimit("uploadChartRequest " + loginUser.getId());

         String chartName = generateChartRequest.getChartName();
         String chartType = generateChartRequest.getChartType();
         String goal = generateChartRequest.getGoal();
         Chart chart = new Chart();
         chart.setUserId(loginUser.getId());
         chart.setChartName(chartName);
         chart.setGoal(goal);
         chart.setChartType(chartType);
         chart.setGenState(ChartGenStateEnum.WAITING.getValue());
         chart.setIsChecked(false);

         String compressedData = ExcelUtils.convertToCSV(multipartFile);
         chart.setChartData(compressedData);

         chartService.save(chart);

         // 传递给消息队列
         mbimqProducer.sendMessage(mbiExchange, chartGenKey, chart.getId().toString());

         return ResultUtils.success(chart.getId());
    }

    @PostMapping("/checked")
    public BaseResponse<Boolean> checkChart(@RequestParam("id") Long chartId) {
        // 设置图标结果被查看过
        chartService.changeCheckState(chartId, true);

        return ResultUtils.success(true);
    }

    /**
     * 编辑（用户）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);

        User loginUser = userService.getLoginUser(request);

        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

}
