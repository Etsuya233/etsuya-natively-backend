package com.dc.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dc.learning.constant.Constant;
import com.dc.learning.constant.RedisConstant;
import com.dc.learning.constant.RegexConstant;
import com.dc.learning.constant.UserStatusConstant;
import com.dc.learning.domain.dto.LoginDto;
import com.dc.learning.domain.dto.RegisterDto;
import com.dc.learning.domain.dto.UserGeneralInfoDto;
import com.dc.learning.domain.po.User;
import com.dc.learning.domain.vo.LoginVo;
import com.dc.learning.enums.ExceptionEnums;
import com.dc.learning.exception.BaseException;
import com.dc.learning.mapper.UserMapper;
import com.dc.learning.service.IUserService;
import com.dc.learning.utils.BaseContext;
import com.dc.learning.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-09-26
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

	private final PasswordEncoder passwordEncoder;
	private final JwtUtils jwtUtils;
	private final StringRedisTemplate redisTemplate;

	@Override
	public LoginVo login(LoginDto loginDto) {
		String username = loginDto.getUsername();
		String password = loginDto.getPassword();

		User user;
		if(Pattern.matches(RegexConstant.PHONE, username)){
			//手机号登录
			user = this.lambdaQuery()
					.eq(User::getPhone, username)
					.one();
		} else {
			//用户名登陆
			user = this.lambdaQuery()
					.eq(User::getUsername, username)
					.one();
		}

		if(user == null){
			throw new BaseException(ExceptionEnums.USER_NOT_FOUND);
		}

		//密码错误
		if(!passwordEncoder.matches(password, user.getPassword())){
			throw new BaseException(ExceptionEnums.PASSWORD_NOT_MATCH);
		}

		//登陆成功
		Map<String, Object> claims = Map.of("userId", user.getId(), "version", user.getVersion());
		String accessToken = jwtUtils.createToken(claims, Constant.ACCESS_TOKEN_TTL);
		String refreshToken = jwtUtils.createToken(claims, Constant.REFRESH_TOKEN_TTL);

		//讲Refresh Token上传至Redis
		redisTemplate.opsForValue().set(RedisConstant.USER_REFRESH_TOKEN_PREFIX + refreshToken, String.valueOf(user.getId()),
				Constant.REFRESH_TOKEN_TTL, TimeUnit.SECONDS);

		return new LoginVo(accessToken, refreshToken);
	}

	@Override
	public void register(RegisterDto registerDto) {
		//检验是否重复
		Long usernameCount = this.lambdaQuery()
				.eq(User::getUsername, registerDto.getUsername())
				.count();
		if(usernameCount > 0){
			throw new BaseException("用户名已被使用", "用户名已被使用");
		}
		if(StrUtil.isNotBlank(registerDto.getPhone())){
			Long phoneCount = this.lambdaQuery()
					.eq(User::getPhone, registerDto.getPhone())
					.count();
			if(phoneCount > 0){
				throw new BaseException("手机号已被使用", "手机号已被使用");
			}
		}
		//注册成功，插入数据库
		User user = BeanUtil.toBean(registerDto, User.class);
		String encodedPassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(encodedPassword);
		this.save(user);
	}

	@Override
	public User getCurrent() {
		Long userId = BaseContext.getUserId();
		User user = this.getById(userId);
		if(user.getStatus() == UserStatusConstant.ERROR){
			throw new BaseException(ExceptionEnums.USER_STATUS_ERROR);
		}
		user.setPassword(null);
		return user;
	}

	@Override
	public User getUserInfo(Long id) {
		User user = this.getById(id);
		if(user == null){
			throw new BaseException(ExceptionEnums.USER_NOT_FOUND);
		} else if(user.getStatus() == UserStatusConstant.ERROR){
			throw new BaseException(ExceptionEnums.USER_STATUS_ERROR);
		}
		//TODO 这里需要一些隐私设置，比如跟据是否暂时某些信息来屏蔽某些字段
		user.setPassword(null);
		user.setPhone(null);
		return user;
	}

	@Override
	public String usernameUnique(String username) {
		Long userId = BaseContext.getUserId();
		if(userId != null){
			User user = this.getById(userId);
			if(user.getUsername().equals(username)){
				return "";
			}
		}
		Long count = this.lambdaQuery()
				.eq(User::getUsername, username)
				.count();
		if(count > 0) return "1";
		else return "";
	}

	@Override
	public void modifyUserInfo(UserGeneralInfoDto dto) {
		Long userId = BaseContext.getUserId();
		User user = this.getById(userId);

		//检验Id是否重复
		if(!user.getUsername().equals(dto.getUsername())){
			Long count = this.lambdaQuery()
					.eq(User::getUsername, dto.getUsername())
					.count();
			if(count > 0) throw new BaseException("用户名已被占用", "用户名已被占用");
		}

		user.setUsername(dto.getUsername());
		user.setNickname(dto.getNickname());
		user.setGender(dto.getGender());
		this.updateById(user);
	}

	@Override
	public String phoneUnique(String phone){
		Long userId = BaseContext.getUserId();
		if(userId != null){
			User user = this.getById(userId);
			if(user.getPhone().equals(phone)){
				return "";
			}
		}
		Long count = this.lambdaQuery()
				.eq(User::getPhone, phone)
				.count();
		if(count > 0) return "1";
		else return "";
	}

	@Override
	public LoginVo refreshUserToken(String refreshToken) {
		//检验有效性
		Map<String, Object> claims = null;
		try {
			claims = jwtUtils.parseToken(refreshToken);
		} catch (Exception e) {
			throw new BaseException(ExceptionEnums.USER_REFRESH_TOKEN_FAILED);
		}
		//查询Redis中是否存在该Token
		String userIdStr = redisTemplate.opsForValue().getAndDelete(RedisConstant.USER_REFRESH_TOKEN_PREFIX + refreshToken);
		if(userIdStr == null){
			throw new BaseException(ExceptionEnums.USER_REFRESH_TOKEN_FAILED);
		}
		//开始刷新
		String accessToken2 = jwtUtils.createToken(claims, Constant.ACCESS_TOKEN_TTL);
		String refreshToken2 = jwtUtils.createToken(claims, Constant.REFRESH_TOKEN_TTL);
		//讲Refresh Token上传至Redis
		redisTemplate.opsForValue().set(RedisConstant.USER_REFRESH_TOKEN_PREFIX + refreshToken2, userIdStr,
				Constant.REFRESH_TOKEN_TTL, TimeUnit.SECONDS);
		//返回
		return new LoginVo(accessToken2, refreshToken2);
	}
}
