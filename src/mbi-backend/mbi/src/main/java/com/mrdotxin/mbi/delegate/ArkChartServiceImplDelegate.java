package com.mrdotxin.mbi.delegate;

import com.mrdotxin.mbi.ai.impl.ArkChartServiceImpl;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class ArkChartServiceImplDelegate extends ArkChartServiceImpl {

    @Value("${AI.example}")
    private String example;

    @Override
    public String doAIChart(List<String> queries) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            return example;
        }

        return example;
    }
}
