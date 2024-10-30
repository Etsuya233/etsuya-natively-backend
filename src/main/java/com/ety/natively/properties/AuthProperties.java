package com.ety.natively.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
@ConfigurationProperties(prefix = "natively.auth")
public class AuthProperties {
	/**
	 * 不用登录就可以反问的路径
	 */
	private List<String> noAuthPath;

	/**
	 * 需要登陆访问的路径，会覆盖未登录状态
	 */
	private List<String> authPath;
}
