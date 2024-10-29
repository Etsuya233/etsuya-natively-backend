package com.dc.learning.config;

import com.dc.learning.properties.JwtProperties;
import com.dc.learning.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

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
