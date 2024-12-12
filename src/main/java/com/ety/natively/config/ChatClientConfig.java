package com.ety.natively.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {

	private final ChatClient.Builder builder;

	@Bean
	public ChatClient translationChatClient() {
		return builder.build();
	}

}
