package io.tcbs.template.config;

import io.tcbs.template.concurrency.SemaphoreTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SemaphoreConfiguration {

    @Bean
    public SemaphoreTemplate semaphoreTemplate() {
        // Cấp tối đa 3 permit đồng thời, bật tính năng Fair (FIFO)
        return new SemaphoreTemplate(3, true);
    }
}
