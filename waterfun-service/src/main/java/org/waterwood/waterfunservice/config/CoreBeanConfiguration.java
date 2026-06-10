package org.waterwood.waterfunservice.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ComponentScan("org.waterwood.waterfunservicecore")
public class CoreBeanConfiguration {
}
