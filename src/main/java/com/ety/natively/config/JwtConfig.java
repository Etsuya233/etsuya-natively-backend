package com.ety.natively.config;

import com.ety.natively.properties.JwtProperties;
import com.ety.natively.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class JwtConfig {

	@Bean
	public JwtUtils jwtUtils(JwtProperties jwtProperties){
		try {
			JwtUtils jwtUtils = new JwtUtils(jwtProperties.getPrivateKeyPath(), jwtProperties.getPublicKeyPath());
			log.info("已成功创建JwtUtils");
			return jwtUtils;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
