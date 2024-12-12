package com.ety.natively.config;

import com.corundumstudio.socketio.*;
import com.ety.natively.constant.RedisConstant;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.UUID;

//@Component
@RequiredArgsConstructor
@Slf4j
@Deprecated
public class SocketIOServerConfig implements CommandLineRunner {

	private final UserUtils userUtils;
	private final StringRedisTemplate redisTemplate;

	@Override
	public void run(String... args) throws Exception {
		// configurations
		Configuration configuration = new Configuration();
		configuration.setPort(8082);
		configuration.setPackagePrefix("com.ety.natively.domain");
		configuration.setEnableCors(true);

		// server
		SocketIOServer server = new SocketIOServer(configuration);
		server.addNamespace("/chat");
		SocketIONamespace chatNamespace = server.getNamespace("/chat");

		chatNamespace.addAuthTokenListener(new AuthTokenListener() {
			@Override
			public AuthTokenResult getAuthTokenResult(Object authToken, SocketIOClient client) {
				Long userId;
				LinkedHashMap<Object, Object> authMap = (LinkedHashMap<Object, Object>) authToken;
				String token = (String) authMap.get("token");
				try {
					userId = userUtils.authenticateUser(token);
					// TODO lua
					String existedSessionId = (String) redisTemplate.opsForHash().get(RedisConstant.SOCKET_IO_USER_TO_UUID, userId);
					if(existedSessionId != null) {
						
					} else {
						redisTemplate.opsForHash().put(RedisConstant.SOCKET_IO_USER_TO_UUID, userId, client.getSessionId().toString());
					}
					return AuthTokenResult.AuthTokenResultSuccess;
				} catch (Exception e){
					return new AuthTokenResult(false, "error!");
				}
			}
		});;

		// events
		chatNamespace.addConnectListener(client -> {

			log.debug("Connected: {}", client);
		});

		chatNamespace.addEventListener("message", String.class, (client, data, ackSender) -> {
			log.debug("Received from {} : {}", BaseContext.getUserId(), data);
			ackSender.sendAckData("success");
		});

		chatNamespace.addDisconnectListener(client -> {
			UUID sessionId = client.getSessionId();
			// get client from uuid
		});

		// run
		log.info("SocketIO Server 已启动...");
		server.start();
	}
}
