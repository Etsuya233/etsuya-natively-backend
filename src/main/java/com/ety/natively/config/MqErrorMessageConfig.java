package com.ety.natively.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
		prefix = "spring.rabbitmq.listener.simple.retry",
		name = "enabled",
		havingValue = "true")
public class MqErrorMessageConfig {
	//创建失败专用的交换机和队列
	@Bean
	public DirectExchange errorDirect(){
		return new DirectExchange("error.direct");
	}
	@Bean
	public Queue errorQueue(){
		return new Queue("error.queue");
	}
	@Bean
	public Binding errorQueueBinding(@Qualifier("errorDirect") DirectExchange directExchange,
									 @Qualifier("errorQueue") Queue errorQueue){
		return BindingBuilder.bind(errorQueue).to(directExchange).with("error");
	}

	//定义MessageRecoverer
	@Bean
	public MessageRecoverer republishMessageRecoverer(RabbitTemplate rabbitTemplate){
		return new RepublishMessageRecoverer(rabbitTemplate, "error.direct", "error");
	}
}
