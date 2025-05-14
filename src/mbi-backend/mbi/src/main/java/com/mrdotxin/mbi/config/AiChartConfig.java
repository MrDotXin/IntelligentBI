package com.mrdotxin.mbi.config;

import com.mrdotxin.mbi.ai.AIChartService;
import com.mrdotxin.mbi.delegate.ArkChartServiceImplDelegate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChartConfig {

    @Bean
    public AIChartService getAiChartService() {
        return new ArkChartServiceImplDelegate();
    }
}
