package com.dc.learning.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MqConfig {
	@Bean
	public MessageConverter messageConverter(){
		log.info("已注册Jackson2JsonMessageConverter");
		return new Jackson2JsonMessageConverter();
	}
}
