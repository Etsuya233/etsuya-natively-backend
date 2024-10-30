package com.ety.natively.utils.openai;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 用于生成文字的AI
 */
@Slf4j
public class TextAi {
	public static TextResponse ai(AiProvider provider, TextRequest request){
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setAll(Map.of("Authorization", "Bearer " + provider.getKey()));

		HttpEntity<TextRequest> requestEntity = new HttpEntity<>(request, httpHeaders);

		ResponseEntity<TextResponse> exchange = new RestTemplate().exchange(
				provider.getUrl(),
				HttpMethod.POST,
				requestEntity,
				TextResponse.class,
				Map.of()
		);

		log.debug(exchange.toString());

		return exchange.getBody();
	}
}
