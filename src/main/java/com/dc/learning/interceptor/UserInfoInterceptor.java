package com.dc.learning.interceptor;

import cn.hutool.core.util.StrUtil;
import com.dc.learning.constant.RedisConstant;
import com.dc.learning.domain.po.User;
import com.dc.learning.enums.ExceptionEnums;
import com.dc.learning.exception.BaseException;
import com.dc.learning.mapper.UserMapper;
import com.dc.learning.properties.AuthProperties;
import com.dc.learning.utils.BaseContext;
import com.dc.learning.utils.JwtUtils;
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
		if(notNeedLogin) return true;

		//开始验证登陆状态
		String header = request.getHeader("Authorization");
		if(StrUtil.isBlank(header) || header.length() < 7){
			throw new BaseException(ExceptionEnums.USER_ACCESS_TOKEN_FAILED);
		}

		String token = header.substring(7); //Bearer ABCDEFG

		//验证Token合法性
		Map<String, Object> claims;
		try {
			claims = jwtUtils.parseToken(token);
		} catch (Exception e) {
			throw new BaseException(ExceptionEnums.USER_ACCESS_TOKEN_FAILED);
		}
		Long userId = (Long) claims.get("userId");
		Byte currentVersion = (Byte) claims.get("version");

		//验证版本合法性
		String versionStr = redisTemplate.opsForValue().get(RedisConstant.USER_VERSION_TOKEN_PREFIX + userId.toString());
		Byte version;
		if(versionStr == null){
			User user = userMapper.selectById(userId);
			version = user.getVersion();
			redisTemplate.opsForValue().set(RedisConstant.USER_VERSION_TOKEN_PREFIX, version.toString());
		} else {
			version = Byte.parseByte(versionStr);
		}
		if(!version.equals(currentVersion)){
			throw new BaseException(ExceptionEnums.USER_ACCESS_TOKEN_FAILED);
		}

		BaseContext.setUserId(userId);

		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		BaseContext.removeUserId();
	}
}
