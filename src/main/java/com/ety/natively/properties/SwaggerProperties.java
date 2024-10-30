package com.ety.natively.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Swagger信息配置类
 */
@Component
@Data
@ConfigurationProperties(prefix = "natively.swagger")
public class SwaggerProperties {
	/**
	 * 标题
	 */
	private String title;

	/**
	 * 描述
	 */
	private String description;

	/**
	 * 版本号
	 */
	private String version;

	/**
	 * 许可证级别
	 */
	private String license;

	/**
	 * 作者
	 */
	private String author;
	/**
	 * 邮箱
	 */
	private String email;

	/**
	 * 扫描包的位置
	 */
	private String basePackage;
}
