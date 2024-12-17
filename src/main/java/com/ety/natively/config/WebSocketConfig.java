package com.ety.natively.config;

import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final UserUtils userUtils;

	/**
	 * 配置Stomp
	 * @param registry registry
	 */
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// 定义了连接WebSocket的端点，前端时使用这个来连接后端
		// 如果不用Stomp，可以使用WebSocketHandler
		registry.addEndpoint("/chat/connect")
				.setAllowedOriginPatterns("*")
				.withSockJS();
	}


	/**
	 * 配置消息Broker，可以配置RabbitMQ什么的。
	 * @param registry registry
	 */
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// 下面一行指明了所有@MessageMapping的前缀，不管@MessageMapping所在Controller的前缀！
		registry.setApplicationDestinationPrefixes("/ws");
		registry.setUserDestinationPrefix("/user");

		// 下面是使用简单的基于内存的SimpleBroker
		// 启用了一个简单的基于内存的Broker。（以后可以换成更高级的MQ！）
		registry.enableSimpleBroker("/topic", "/queue");

		// 下面是使用外置的RabbitMQ
//		registry.enableStompBrokerRelay("/exchange", "/topic", "/queue", "/amq/queue")
//				.setVirtualHost("/natively")
//				.setRelayHost("localhost")
//				.setRelayPort(61613)
//				.setClientLogin("natively")
//				.setClientPasscode("ety2004")
//				.setSystemLogin("natively")
//				.setSystemPasscode("ety2004");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor =
						MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

				log.debug("{}: {}", accessor.getSessionId(), accessor.getCommand());

				if(StompCommand.CONNECT.equals(accessor.getCommand())) {
					List<String> authorizationHeaders = accessor.getNativeHeader("Authorization");
					if(authorizationHeaders == null || authorizationHeaders.isEmpty()) {
						throw new BaseException(ExceptionEnum.USER_ACCESS_TOKEN_FAILED);
					}
					String token = authorizationHeaders.getFirst();
					token = token.substring(7);
					Long userId = userUtils.authenticateUser(token);
					accessor.setUser(() -> String.valueOf(userId));
					BaseContext.setUserId(userId);
					log.debug("用户 {} 已连接至WebSocket", userId);
				}

				else if(StompCommand.DISCONNECT.equals(accessor.getCommand())) {
					log.debug("Disconnected: {}", accessor.getSessionId());
				}

				return message;
			}
		});
	}


}