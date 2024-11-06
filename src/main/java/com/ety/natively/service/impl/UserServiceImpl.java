package com.ety.natively.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ety.natively.constant.Constant;
import com.ety.natively.constant.RedisConstant;
import com.ety.natively.constant.RegexConstant;
import com.ety.natively.constant.UserStatusConstant;
import com.ety.natively.domain.dto.*;
import com.ety.natively.domain.po.User;
import com.ety.natively.domain.po.UserLanguage;
import com.ety.natively.domain.po.UserOauth;
import com.ety.natively.domain.vo.LoginVo;
import com.ety.natively.domain.vo.OAuth2LoginVo;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.mapper.UserLanguageMapper;
import com.ety.natively.mapper.UserMapper;
import com.ety.natively.properties.MinioProperties;
import com.ety.natively.properties.OAuth2Properties;
import com.ety.natively.service.GeneralService;
import com.ety.natively.service.IUserOauthService;
import com.ety.natively.service.IUserService;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.JwtUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Person;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
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
	private final OAuth2Properties oAuth2Properties;
	private final MinioClient minioClient;
	private final IUserOauthService userOauthService;
	private final MinioProperties minioProperties;

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

		return loginSuccess(user);
	}

	@NotNull
	private LoginVo loginSuccess(User user) {
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
	@Transactional
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
		//检查是否是OAuth2注册
		if(registerDto.getOwner() != null){
			switch(registerDto.getOwner()){
				case "google" -> oAuth2GoogleRegisterComplete(registerDto.getOwnerId(), user.getId());
			}
		}
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
		user.setAvatar(minioProperties.getPublicPrefix() + user.getAvatar());
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
		user.setAvatar(minioProperties.getPublicPrefix() + user.getAvatar());
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


	private final List<String> oauthGoogleScopes = List.of(PeopleServiceScopes.USERINFO_PROFILE, PeopleServiceScopes.USERINFO_EMAIL);
	private final HttpTransport httpTransport = new NetHttpTransport();
	private final GsonFactory gsonFactory = GsonFactory.getDefaultInstance();
	private final PeopleService peopleService = new PeopleService.Builder(
			httpTransport, gsonFactory, null)
			.setApplicationName("Natively").build();

	@Override
	@Transactional
	public OAuth2LoginVo oAuth2Login(OAuth2Request request) {
		String owner = request.getOwner();
		String code = request.getCode();

		switch (owner){
			case "google": return oAuth2GoogleRegister(code);
			case "github": return oAuth2GithubRegister(code);
		}

		return null;
	}

	private OAuth2LoginVo oAuth2GithubRegister(String code) {
		OAuth2Properties.OAuth2Config config = oAuth2Properties.getProvider().get("github");
		RestTemplate restTemplate = new RestTemplate();

		Map<String, String> body = Map.of(
				"client_id", config.getClientId(),
				"client_secret", config.getClientSecret(),
				"redirect_uri", config.getRedirectUri(),
				"code", code);

		GithubAccessTokenDto res;
		try {
			URI accessTokenUrl = new URI("https://github.com/login/oauth/access_token");
			RequestEntity<Map<String, String>> requestEntity = new RequestEntity<>(body, HttpMethod.POST, accessTokenUrl);
			ResponseEntity<GithubAccessTokenDto> responseEntity = restTemplate.exchange(requestEntity, GithubAccessTokenDto.class);
			if(!responseEntity.getStatusCode().is2xxSuccessful()) throw new RuntimeException("github登录获取access_token响应体为空");
			res = responseEntity.getBody();
		} catch (Exception e){
			throw new RuntimeException(e);
		}

		Map<String, String> userInfoHeader = Map.of(
				"Accept", "application/vnd.github+json",
				"Authorization", "Bearer " + res.getAccessToken(),
				"X-GitHub-Api-Version", "2022-11-28");
		Map<String, String> userInfoRes;
		try {
			URI userInfoUri = new URI("https://github.com/login/oauth/access_token");
			RequestEntity<Map<String, String>> requestEntity = new RequestEntity<>(userInfoHeader, HttpMethod.GET, userInfoUri);
			ResponseEntity<Map<String, String>> responseEntity = restTemplate.exchange(requestEntity, new ParameterizedTypeReference<Map<String, String>>() {});
			if(!responseEntity.getStatusCode().is2xxSuccessful()) throw new RuntimeException();
			userInfoRes = responseEntity.getBody();
		} catch (Exception e){
			throw new RuntimeException(e);
		}

		String githubId = userInfoRes.get("id");
		String nickname = userInfoRes.get("name");
		String avatar = userInfoRes.get("avatar_url");
		String email = userInfoRes.get("email");





		return null;
	}

	private OAuth2LoginVo oAuth2GoogleRegister(String code) {
		OAuth2Properties.OAuth2Config config = oAuth2Properties.getProvider().get("google");

		//get access token
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport,
				gsonFactory,
				config.getClientId(),
				config.getClientSecret(),
				oauthGoogleScopes
		).setAccessType("offline").build();
		Person person;
		try {
			GoogleTokenResponse response;
			response = flow.newTokenRequest(code)
					.setRedirectUri(config.getRedirectUri())
					.setScopes(oauthGoogleScopes)
					.setGrantType("authorization_code")
					.execute();
			String accessToken = response.getAccessToken();
			person = peopleService.people().get("people/me")
					.setAccessToken(accessToken)
					.setPersonFields("names,emailAddresses,photos,biographies,genders,locales,locations")
					.execute();
		} catch (Exception e){
			throw new RuntimeException(e);
		}

		//extract google id
		String resourceName = person.getResourceName();
		String googleId = resourceName.substring(resourceName.lastIndexOf('/') + 1);

		//user exists?
		UserOauth userOauth = userOauthService.lambdaQuery()
				.eq(UserOauth::getOwner, "google")
				.eq(UserOauth::getOwnerId, googleId)
				.one();

		String redisKey = RedisConstant.USER_OAUTH + "google:" + googleId;

		if(userOauth == null){ //new user

			//get things
			String nickname = person.getNames().getFirst().getDisplayName(); //as nickname
			String avatarUrlGoogle = person.getPhotos().getFirst().getUrl(); //avatar
			String email = null; //email
			for(EmailAddress emailAddress: person.getEmailAddresses()){
				if(emailAddress.getMetadata().getPrimary()){
					email = emailAddress.getValue();
					break;
				}
			}

			OAuth2RegisterDto registerDto = new OAuth2RegisterDto(nickname, email, avatarUrlGoogle, googleId, "google");
			redisTemplate.opsForValue()
					.set(redisKey, JSONUtil.toJsonStr(registerDto), RedisConstant.USER_OAUTH_TTL, TimeUnit.SECONDS);
			return new OAuth2LoginVo(false, registerDto, null);

		} else { //login
			User user = this.lambdaQuery()
					.eq(User::getId, userOauth.getUserId())
					.one();
			LoginVo loginVo = loginSuccess(user);
			return new OAuth2LoginVo(true, null, loginVo);
		}
	}

	@Transactional
	void oAuth2GoogleRegisterComplete(String googleId, Long userId){
		//get avatar
		String registerDtoJson = redisTemplate.opsForValue()
				.getAndDelete(RedisConstant.USER_OAUTH + "google:" + googleId);
		OAuth2RegisterDto registerDto = JSONUtil.toBean(registerDtoJson, OAuth2RegisterDto.class);

		String avatarUrlGoogle = registerDto.getAvatarUrl();
		String avatarUrl;
		String avatarFileName = UUID.randomUUID().toString();
		try {
			URL url = new URI(avatarUrlGoogle).toURL();
			URLConnection connection = url.openConnection();
			InputStream is = connection.getInputStream();
			PutObjectArgs args = PutObjectArgs.builder()
					.bucket("natively")
					.object(avatarFileName)
					.stream(is, connection.getContentLengthLong(), -1)
					.contentType("application/octet-stream")
					.build();
			minioClient.putObject(args);
			avatarUrl = "/natively/" + avatarFileName;
			is.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		//update and insert into db
		this.lambdaUpdate()
				.eq(User::getId, userId)
				.set(User::getAvatar, avatarUrl)
				.update();
		UserOauth userOauth = new UserOauth(null, userId, "google", googleId, true, null, null);
		userOauthService.save(userOauth);
	}


}
