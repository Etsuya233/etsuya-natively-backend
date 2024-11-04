package com.ety.natively.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.ety.natively.constant.Constant;
import com.ety.natively.constant.RedisConstant;
import com.ety.natively.constant.RegexConstant;
import com.ety.natively.constant.UserStatusConstant;
import com.ety.natively.domain.dto.LoginDto;
import com.ety.natively.domain.dto.RegisterDto;
import com.ety.natively.domain.dto.UserInfoModificationDto;
import com.ety.natively.domain.dto.UserRefreshDto;
import com.ety.natively.domain.po.User;
import com.ety.natively.domain.po.UserLanguage;
import com.ety.natively.domain.vo.LoginVo;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.mapper.UserLanguageMapper;
import com.ety.natively.mapper.UserMapper;
import com.ety.natively.service.GeneralService;
import com.ety.natively.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.JwtUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-30
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

	private final PasswordEncoder passwordEncoder;
	private final JwtUtils jwtUtils;
	private final StringRedisTemplate redisTemplate;
	private final GeneralService generalService;
	private final UserLanguageMapper userLanguageMapper;

	private Set<String> languageCodes;
	private final Set<String> locations = new HashSet<>();

	@PostConstruct
	public void init() {
		languageCodes = generalService.getLanguageCodes();
		for (Locale locale : Locale.getAvailableLocales()) {
			locations.add(locale.getCountry());
		}
	}

	@Override
	public LoginVo login(LoginDto loginDto) {
		String username = loginDto.getUsername();
		String password = loginDto.getPassword();

		User user;
		if(Pattern.matches(RegexConstant.EMAIL, username)){
			//邮箱登录
			user = this.lambdaQuery()
					.eq(User::getEmail, username)
					.one();
		} else {
			//用户名登陆
			user = this.lambdaQuery()
					.eq(User::getUsername, username)
					.one();
		}

		if(user == null){
			throw new BaseException(ExceptionEnum.USER_NOT_FOUND);
		}

		//密码错误
		if(!passwordEncoder.matches(password, user.getPassword())){
			throw new BaseException(ExceptionEnum.USER_PASSWORD_NOT_MATCH);
		}

		//登陆成功
		Map<String, Object> claims = Map.of("userId", user.getId(),
											"version", user.getVersion());
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
			throw new BaseException(ExceptionEnum.USER_USERNAME_TAKEN);
		}
		if(StrUtil.isNotBlank(registerDto.getEmail())){
			Long phoneCount = this.lambdaQuery()
					.eq(User::getEmail, registerDto.getEmail())
					.count();
			if(phoneCount > 0){
				throw new BaseException(ExceptionEnum.USER_EMAIL_TAKEN);
			}
		}
		//检查地区
		if(!locations.contains(registerDto.getLocation())){
			throw new BaseException(ExceptionEnum.USER_LOCATION_FAILED);
		}
		//检查语言列表
		ArrayList<UserLanguage> languages = new ArrayList<>();
		for (RegisterDto.LanguageSelection selection : registerDto.getLanguage()) {
			if(selection.getProficiency() <= 0 || selection.getProficiency() > 5){
				throw new BaseException(ExceptionEnum.USER_LANGUAGE_PROFICIENCY_FAILED);
			}
			if(!languageCodes.contains(selection.getLanguage())){
				throw new BaseException(ExceptionEnum.USER_LANGUAGE_FAILED);
			}
			languages.add(new UserLanguage(null, null, selection.getLanguage(), selection.getProficiency()));
		}
		//注册成功，插入数据库
		User user = BeanUtil.toBean(registerDto, User.class);
		String encodedPassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(encodedPassword);
		this.save(user);
		languages.forEach(lang -> lang.setUserId(user.getId()));
		userLanguageMapper.insert(languages);
	}

	@Override
	public LoginVo refreshUserToken(UserRefreshDto dto) {
		String refreshToken = dto.getRefreshToken();
		//检验有效性
		Map<String, Object> claims = null;
		try {
			claims = jwtUtils.parseToken(refreshToken);
		} catch (Exception e) {
			throw new BaseException(ExceptionEnum.USER_REFRESH_TOKEN_FAILED);
		}
		//查询Redis中是否存在该Token
		String userIdStr = redisTemplate.opsForValue().getAndDelete(RedisConstant.USER_REFRESH_TOKEN_PREFIX + refreshToken);
		if(userIdStr == null){
			throw new BaseException(ExceptionEnum.USER_REFRESH_TOKEN_FAILED);
		}
		//开始刷新，如果版本号老了就抛出异常
		Long userId = (Long) claims.get("userId");
		Integer currentVersion = (Integer) claims.get("version"); //反正解析出来是Integer
		String versionStr = redisTemplate.opsForValue().get(RedisConstant.USER_VERSION_TOKEN_PREFIX + userId.toString());
		Integer version;
		User user = this.getById(userId);
		if(versionStr == null){
			version = user.getVersion().intValue();
			redisTemplate.opsForValue().set(RedisConstant.USER_VERSION_TOKEN_PREFIX + userId, version.toString(),
					RedisConstant.USER_VERSION_TTL, TimeUnit.SECONDS);
		} else {
			version = Integer.parseInt(versionStr);
		}
		if(!version.equals(currentVersion)){
			throw new BaseException(ExceptionEnum.USER_REFRESH_TOKEN_FAILED);
		}
		String accessToken2 = jwtUtils.createToken(claims, Constant.ACCESS_TOKEN_TTL);
		String refreshToken2 = jwtUtils.createToken(claims, Constant.REFRESH_TOKEN_TTL);
		//Refresh Token上传至Redis
		redisTemplate.opsForValue().set(RedisConstant.USER_REFRESH_TOKEN_PREFIX + refreshToken2, userIdStr,
				Constant.REFRESH_TOKEN_TTL, TimeUnit.SECONDS);
		//返回
		return new LoginVo(accessToken2, refreshToken2);
	}

	@Override
	public User getCurrent() {
		Long userId = BaseContext.getUserId();
		User user = this.getById(userId);
		if(user.getStatus() == UserStatusConstant.ERROR){
			throw new BaseException(ExceptionEnum.USER_STATUS_ERROR);
		}
		user.setPassword(null);
		return user;
	}

	@Override
	public User getUserInfo(Long id) {
		User user = this.getById(id);
		if(user == null){
			throw new BaseException(ExceptionEnum.USER_NOT_FOUND);
		} else if(user.getStatus() == UserStatusConstant.ERROR){
			throw new BaseException(ExceptionEnum.USER_STATUS_ERROR);
		}
		//TODO 这里需要一些隐私设置，比如跟据是否暂时某些信息来屏蔽某些字段
		user.setPassword(null);
		user.setEmail(null);
		return user;
	}

	@Override
	public Boolean usernameUnique(String username) {
		Long userId = BaseContext.getUserId();
		if(userId != null){
			User user = this.getById(userId);
			if(user.getUsername().equals(username)){
				return true;
			}
		}
		Long count = this.lambdaQuery()
				.eq(User::getUsername, username)
				.count();
		return count <= 0;
	}

	@Override
	public Boolean emailUnique(String email) {
		Long userId = BaseContext.getUserId();
		if(userId != null){
			User user = this.getById(userId);
			if(user.getEmail().equals(email)){
				return true;
			}
		}
		Long count = this.lambdaQuery()
				.eq(User::getEmail, email)
				.count();
		return count <= 0;
	}

	@Override
	public void modifyUserInfo(UserInfoModificationDto dto) {
		Boolean unique = usernameUnique(dto.getUsername());
		if(!unique){
			throw new BaseException(ExceptionEnum.USER_USERNAME_TAKEN);
		}
		User user = BeanUtil.toBean(dto, User.class);
		user.setId(BaseContext.getUserId());
		this.updateById(user);
	}


}
