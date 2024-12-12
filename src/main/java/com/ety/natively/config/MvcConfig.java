package com.ety.natively.config;

import com.ety.natively.interceptor.LocaleInterceptor;
import com.ety.natively.interceptor.UserInfoInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class MvcConfig implements WebMvcConfigurer {

	private final UserInfoInterceptor userInfoInterceptor;

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

	@Bean
	public RestTemplate restTemplate(){
		return new RestTemplate();
	}

	@Bean
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);  // 最小线程数
		executor.setMaxPoolSize(10);  // 最大线程数
		executor.setQueueCapacity(100);  // 队列容量
		executor.setThreadNamePrefix("async-executor-");
		executor.initialize();
		return executor;
	}

	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		configurer.setTaskExecutor(taskExecutor());
		configurer.setDefaultTimeout(300000);  // 默认超时时间，单位是毫秒
	}

}
