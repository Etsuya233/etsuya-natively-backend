package com.ety.natively.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "natively.oauth2")
@Data
public class OAuth2Properties {

	Map<String, OAuth2Config> provider;

	@Data
	public static class OAuth2Config {
		private String clientId;
		private String clientSecret;
		private String redirectUri;
	}
}
