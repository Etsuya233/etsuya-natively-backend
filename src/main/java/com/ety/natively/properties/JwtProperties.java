package com.ety.natively.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置类
 */
@ConfigurationProperties(prefix = "natively.jwt")
@Component
@Data
public class JwtProperties {
	/**
	 * 公钥位置，Classpath
	 */
	private String publicKeyPath;
	/**
	 * 私钥位置，Classpath
	 */
	private String privateKeyPath;
}