package com.mrdotxin.mbi.bizmq;

import com.mrdotxin.mbi.common.ErrorCode;
import com.mrdotxin.mbi.exception.BusinessException;
import com.mrdotxin.mbi.exception.ThrowUtils;
import com.mrdotxin.mbi.model.dto.chart.BIChartResponse;
import com.mrdotxin.mbi.model.entity.Chart;
import com.mrdotxin.mbi.model.enums.ChartGenStateEnum;
import com.mrdotxin.mbi.ai.AIChartService;
import com.mrdotxin.mbi.service.ChartService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Objects;


@Component
public class MBIMQConsumer {

    private static final Logger log = LoggerFactory.getLogger(MBIMQConsumer.class);

    @Resource
    private ChartService chartService;

    @Resource
    private AIChartService aiChartService;


    @RabbitListener(queues = "ChartGenQueue", ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        try {
            long chartId = Long.parseLong(message);
            try {
                Chart chart = chartService.getById(chartId);
                ThrowUtils.throwIf(Objects.isNull(chart), new RuntimeException("请求的数据不存在"));
                chartService.changeGenState(chartId, ChartGenStateEnum.GENERATING.getValue());

                List<String> prompt = chartService.buildChartAiPrompt(chart);

                String outcome = aiChartService.doAIChart(prompt);
                BIChartResponse biChartResponse = chartService.buildResponseWithGenResult(outcome);

                chartService.updateGenResultAndState(chart.getId(), biChartResponse.getChartResult(), biChartResponse.getChartData());

                channel.basicAck(tag, false);
            } catch (Exception e) {
                channel.basicNack(tag, false, false);
                chartService.changeGenState(chartId, ChartGenStateEnum.ERROR.getValue());
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法生成 " + e.getMessage() + " 请重试!");
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法生成 " + e.getMessage() + " 请重试!");
        }
    }

}
