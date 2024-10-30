package com.ety.natively.config;

import com.ety.natively.properties.SwaggerProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Resource
	private SwaggerProperties swaggerProperties;

	@Bean
	public OpenAPI openAPI(){
		return new OpenAPI().info(new Info()
				.contact(new Contact().email(swaggerProperties.getEmail()).name(swaggerProperties.getAuthor()))
				.version(swaggerProperties.getVersion())
				.title(swaggerProperties.getTitle())
				.description(swaggerProperties.getDescription())
				.license(new License().name(swaggerProperties.getLicense()))
		);
	}
}
