package com.ety.natively.utils;

import com.ety.natively.constant.RedisConstant;
import com.ety.natively.domain.po.User;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class UserUtils {

	private final JwtUtils jwtUtils;
	private final UserMapper userMapper;
	private final StringRedisTemplate redisTemplate;

	public Long authenticateUser(String token){
		//验证Token合法性
		Map<String, Object> claims;
		try {
			claims = jwtUtils.parseToken(token);
		} catch (Exception e) {
			throw new BaseException(ExceptionEnum.USER_ACCESS_TOKEN_FAILED);
		}
		Long userId = (Long) claims.get("userId");
		Integer currentVersion = (Integer) claims.get("version");

		//验证版本合法性
		String versionStr = redisTemplate.opsForValue().get(RedisConstant.USER_VERSION_TOKEN_PREFIX + userId.toString());
		Integer version;
		if(versionStr == null){
			User user = userMapper.selectById(userId);
			version = user.getVersion();
			redisTemplate.opsForValue().set(RedisConstant.USER_VERSION_TOKEN_PREFIX + userId, version.toString(),
					RedisConstant.USER_VERSION_TTL,
					TimeUnit.SECONDS);
		} else {
			version = Integer.parseInt(versionStr);
		}
		if(!version.equals(currentVersion)){
			throw new BaseException(ExceptionEnum.USER_ACCESS_TOKEN_FAILED);
		}

		return userId;
	}

}
