package com.trainergameserver.round.rule;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** round.rule.* 설정을 RoundRuleProperties 빈으로 등록 */
@Configuration
@EnableConfigurationProperties(RoundRuleProperties.class)
public class RoundRuleConfig {
}
