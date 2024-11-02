package com.ety.natively.config;

import com.ety.natively.interceptor.LocaleInterceptor;
import com.ety.natively.interceptor.UserInfoInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class MvcConfig implements WebMvcConfigurer {

	private final UserInfoInterceptor userInfoInterceptor;

//	@Override
//	public void addCorsMappings(CorsRegistry registry) {
//		log.info("已配置跨域");
//		registry.addMapping("/**")
//				.allowedOriginPatterns("*")
//				.allowCredentials(true)
//				.allowedMethods("*")
//				.maxAge(3600);
//	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		String[] excludePatterns = new String[]{"/swagger-resources/**", "/webjars/**", "/v3/**", "/swagger-ui.html/**",
				"/api", "/api-docs", "/api-docs/**", "/doc.html/**", "/swagger-ui/**", "/favicon.ico"};
		registry.addInterceptor(userInfoInterceptor)
				.addPathPatterns("/**")
				.excludePathPatterns(excludePatterns);
		registry.addInterceptor(new LocaleInterceptor())
				.addPathPatterns("/**")
				.order(-1);
	}

	@Bean
	public PasswordEncoder passwordEncoder(){
		log.info("已注册BCryptPasswordEncoder");
		return new BCryptPasswordEncoder();
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/static/favicon.ico");
	}
}
