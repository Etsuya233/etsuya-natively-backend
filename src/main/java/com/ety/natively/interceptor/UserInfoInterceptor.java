package com.ety.natively.interceptor;

import cn.hutool.core.util.StrUtil;
import com.ety.natively.constant.RedisConstant;
import com.ety.natively.domain.po.User;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.mapper.UserMapper;
import com.ety.natively.properties.AuthProperties;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.JwtUtils;
import com.ety.natively.utils.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserInfoInterceptor implements HandlerInterceptor {

	private final JwtUtils jwtUtils;
	private final AuthProperties authProperties;
	private final AntPathMatcher antPathMatcher = new AntPathMatcher();
	private final StringRedisTemplate redisTemplate;
	private final UserMapper userMapper;
	private final UserUtils userUtils;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		//判断当前路径是否需要登陆
		String uri = request.getRequestURI();
		boolean notNeedLogin = false;
		if(authProperties.getNoAuthPath() != null)
			notNeedLogin = authProperties.getNoAuthPath()
				.stream()
				.anyMatch(s -> antPathMatcher.match(s, uri));
		if(authProperties.getAuthPath() != null)
			notNeedLogin &= authProperties.getAuthPath()
				.stream()
				.noneMatch(s -> antPathMatcher.match(s, uri));

		//开始验证登陆状态
		Long userId = null;
		try {
			String header = request.getHeader("Authorization");
			if(StrUtil.isBlank(header) || header.length() < 7){
				throw new BaseException(ExceptionEnum.USER_ACCESS_TOKEN_FAILED);
			}
			String token = header.substring(7); //Bearer ABCDEFG
			userId = userUtils.authenticateUser(token);
		} catch (Exception e){
			if(!notNeedLogin){
				throw new BaseException(ExceptionEnum.USER_ACCESS_TOKEN_FAILED);
			}
		}

		//获取信息
		BaseContext.setUserId(userId);

		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		BaseContext.removeUserId();
	}
}
