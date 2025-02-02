package com.ety.natively.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ety.natively.constant.*;
import com.ety.natively.domain.dto.*;
import com.ety.natively.domain.po.User;
import com.ety.natively.domain.po.UserLanguage;
import com.ety.natively.domain.po.UserOauth;
import com.ety.natively.domain.po.UserRelationship;
import com.ety.natively.domain.vo.FollowVo;
import com.ety.natively.domain.vo.LoginVo;
import com.ety.natively.domain.vo.OAuth2LoginVo;
import com.ety.natively.domain.vo.UserVo;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.mapper.UserLanguageMapper;
import com.ety.natively.mapper.UserMapper;
import com.ety.natively.properties.MinioProperties;
import com.ety.natively.properties.OAuth2Properties;
import com.ety.natively.service.*;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.JwtUtils;
import com.ety.natively.utils.MinioUtils;
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
import org.springframework.http.HttpHeaders;
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
import java.util.stream.Collectors;

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
	private final RestTemplate restTemplate;
	private final MinioUtils minioUtils;
	private final IUserLanguageService userLanguageService;
	private final IUserRelationshipService userRelationshipService;

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
		BaseContext.setUserId(null);
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

		return new LoginVo(accessToken, refreshToken, user.getId());
	}

	@Override
	@Transactional
	public void register(RegisterDto registerDto) {
		BaseContext.setUserId(null);
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
			oAuth2RegisterComplete(registerDto.getOwner(), registerDto.getOwnerId(), user.getId());
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
		return new LoginVo(accessToken2, refreshToken2, userId);
	}

	@Override
	public UserVo getCurrent() {
		Long id = BaseContext.getUserId();
		return getUserInfo(id);
	}

	@Override
	public UserVo getUserInfo(Long id) {
		Long userId = BaseContext.getUserId();

		User user = this.getById(id);
		user.setAvatar(minioUtils.generateFileUrl(MinioConstant.AVATAR_BUCKET, user.getAvatar()));
		//TODO 这里需要一些隐私设置，比如跟据是否暂时某些信息来屏蔽某些字段
		user.setPassword(null);
		user.setEmail(null);
		user.setVersion(0);
		// 用户语言信息
		List<UserLanguage> langs = userLanguageService.lambdaQuery()
				.eq(UserLanguage::getUserId, id)
				.list();
		// vo
		UserVo userVo = UserVo.of(user, langs);

		return userVo;
	}

	@Override
	public UserVo getUserInfoWithExtra(Long id){
		Long userId = BaseContext.getUserId();
		// 是否关注
		UserRelationship relationship = userRelationshipService.lambdaQuery()
				.eq(UserRelationship::getFollowerId, userId)
				.eq(UserRelationship::getFolloweeId, id)
				.one();
		UserRelationship relationship2 = userRelationshipService.lambdaQuery()
				.eq(UserRelationship::getFollowerId, id)
				.eq(UserRelationship::getFolloweeId, userId)
				.one();

		UserVo userVo = this.getUserInfo(id);

		if (relationship2 != null && relationship2.getStatus().equals(UserRelationshipStatus.BLOCKED)) { // 对方屏蔽我
			UserVo vo = new UserVo();
			vo.setUsername(userVo.getUsername());
			vo.setId(id);
			vo.setRelationship(UserVo.RelationshipStatus.BLOCKED_BY_OTHER);
			userVo = vo;
		} else if (relationship != null && relationship.getStatus().equals(UserRelationshipStatus.BLOCKED)) { // 我屏蔽对方
			userVo.setRelationship(UserVo.RelationshipStatus.BLOCKED_BY_ME);
		} else if(relationship == null && relationship2 == null){
			userVo.setRelationship(UserVo.RelationshipStatus.NO_RELATION);
		} else if(relationship != null && relationship2 == null){
			userVo.setRelationship(UserVo.RelationshipStatus.ONE_WAY_FOLLOW);
		} else if(relationship == null){
			userVo.setRelationship(UserVo.RelationshipStatus.ONE_WAY_FOLLOW_BY_OTHER);
		} else {
			userVo.setRelationship(UserVo.RelationshipStatus.MUTUAL_FOLLOW);
		}

		// 返回
		return userVo;
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

	@Override
	public List<UserVo> getUserByIds(List<Long> ids) {
		if(ids == null || ids.isEmpty()){
			return Collections.emptyList();
		}
		List<User> users = this.lambdaQuery()
				.in(User::getId, ids)
				.list();
		Map<Long, List<UserLanguage>> userLangs = userLanguageService.lambdaQuery()
				.in(UserLanguage::getUserId, ids)
				.list()
				.stream()
				.collect(Collectors.groupingBy(UserLanguage::getUserId, Collectors.toList()));
		return users.stream().map(user -> {
			user.setAvatar(minioUtils.generateFileUrl(MinioConstant.AVATAR_BUCKET, user.getAvatar()));
			return UserVo.of(user, userLangs.get(user.getId()));
		}).toList();
	}

	@Override
	public List<UserVo> getContacts(Long lastId) {
		Long userId = BaseContext.getUserId();
		List<UserRelationship> contacts = userRelationshipService.lambdaQuery()
				.lt(lastId != null, UserRelationship::getFolloweeId, lastId)
				.eq(UserRelationship::getFollowerId, userId)
				.eq(UserRelationship::getStatus, UserRelationshipStatus.MUTUAL_FOLLOWING)
				.last("limit 20")
				.list();
		if(contacts == null || contacts.isEmpty()){
			return Collections.emptyList();
		}
		List<Long> contactIds = contacts.stream()
				.map(UserRelationship::getFolloweeId)
				.toList();
		return this.getUserByIds(contactIds);
	}

	@Override
	public boolean checkIsContact(long senderId, long receiverId) {
		Long count = userRelationshipService.lambdaQuery()
				.eq(UserRelationship::getFollowerId, senderId)
				.eq(UserRelationship::getFolloweeId, receiverId)
				.count();
		return count > 0;
	}

	//TODO 注意边界检查
	@Override
	@Transactional
	public FollowVo follow(UserFollowDto dto) {
		Long userId = BaseContext.getUserId();
		Long followeeId = dto.getFolloweeId();
		FollowVo followVo = new FollowVo();
		Boolean follow = dto.getFollow();  // 获取 follow 参数来判断是关注还是取消关注
		if (followeeId == null){
			throw new BaseException();
		}

		UserRelationship relationshipFollower = userRelationshipService.lambdaQuery()
				.eq(UserRelationship::getFollowerId, userId)
				.eq(UserRelationship::getFolloweeId, followeeId)
				.one();
		UserRelationship relationshipFollowee = userRelationshipService.lambdaQuery()
				.eq(UserRelationship::getFollowerId, followeeId)
				.eq(UserRelationship::getFolloweeId, userId)
				.one();

		if (relationshipFollower != null && relationshipFollower.getStatus().equals(UserRelationshipStatus.BLOCKED)) {
			throw new BaseException(ExceptionEnum.USER_CANNOT_FOLLOW_WHEN_YOU_BLOCKED_SOMEONE);
		} else if (relationshipFollowee != null && relationshipFollowee.getStatus().equals(UserRelationshipStatus.BLOCKED)) {
			throw new BaseException(ExceptionEnum.USER_YOU_ARE_BLOCKED);
		}

		// 如果是关注操作
		if (Boolean.TRUE.equals(follow)) {
			// 当前用户没关注对方，现在关注
			if (relationshipFollower == null) {
				relationshipFollower = new UserRelationship();
				relationshipFollower.setFollowerId(userId);
				relationshipFollower.setFolloweeId(followeeId);
				if (relationshipFollowee != null) {
					relationshipFollower.setStatus(UserRelationshipStatus.MUTUAL_FOLLOWING);
					relationshipFollowee.setStatus(UserRelationshipStatus.MUTUAL_FOLLOWING);
					userRelationshipService.updateById(relationshipFollowee);
				} else {
					relationshipFollower.setStatus(UserRelationshipStatus.FOLLOWING);
				}
				userRelationshipService.save(relationshipFollower);
				this.lambdaUpdate()
						.eq(User::getId, userId)
						.setIncrBy(User::getFollowing, 1)
						.update();
				this.lambdaUpdate()
						.eq(User::getId, followeeId)
						.setIncrBy(User::getFollowers, 1)
						.update();
			}
			if (relationshipFollowee != null) {
				followVo.setRelationship(UserVo.RelationshipStatus.MUTUAL_FOLLOW);
			} else {
				followVo.setRelationship(UserVo.RelationshipStatus.ONE_WAY_FOLLOW);
			}
		} else { // 如果是取消关注操作
			// 当前用户已经关注对方，现在取消关注
			if (relationshipFollower != null) {
				userRelationshipService.removeById(relationshipFollower.getId());
				// 取消关注后，需要检查对方的状态
				if (relationshipFollowee != null) {
					relationshipFollowee.setStatus(UserRelationshipStatus.FOLLOWING);
					userRelationshipService.updateById(relationshipFollowee);
				}
				this.lambdaUpdate()
						.eq(User::getId, userId)
						.setIncrBy(User::getFollowing, -1)
						.update();
				this.lambdaUpdate()
						.eq(User::getId, followeeId)
						.setIncrBy(User::getFollowers, -1)
						.update();
			}
			if (relationshipFollowee != null) {
				followVo.setRelationship(UserVo.RelationshipStatus.ONE_WAY_FOLLOW_BY_OTHER);
			} else {
				followVo.setRelationship(UserVo.RelationshipStatus.NO_RELATION);
			}
		}

		User user = this.getById(followeeId);
		followVo.setFollowers(user.getFollowers());
		followVo.setFollowing(user.getFollowing());
		return followVo;
	}

	//------------------- OAuth2 -------------------

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

		return switch (owner) {
			case "google" -> oAuth2GoogleAuthentication(code);
			case "github" -> oAuth2GithubAuthentication(code);
			case "gitee" -> oAuth2GiteeAuthentication(code);
			default -> null;
		};
	}

	private OAuth2LoginVo oAuth2GiteeAuthentication(String code) {
		OAuth2Properties.OAuth2Config config = oAuth2Properties.getProvider().get("gitee");

		Map<String, String> body = Map.of(
				"client_id", config.getClientId(),
				"client_secret", config.getClientSecret(),
				"redirect_uri", config.getRedirectUri(),
				"code", code,
				"grant_type", "authorization_code");

		Map<String, String> res;
		try {
			URI accessTokenUrl = new URI("https://gitee.com/oauth/token");
			RequestEntity<Map<String, String>> requestEntity = RequestEntity.post(accessTokenUrl).body(body);
			ResponseEntity<Map<String, String>> responseEntity = restTemplate.exchange(requestEntity, new ParameterizedTypeReference<>(){});
			if(!responseEntity.getStatusCode().is2xxSuccessful()) throw new RuntimeException("gitee登录获取access_token响应体为空");
			res = responseEntity.getBody();
		} catch (Exception e){
			throw new RuntimeException(e);
		}

		if(res == null || res.get("access_token") == null){
			throw new RuntimeException();
		}

		Map<String, Object> userInfoRes;
		try {
			URI userInfoUri = new URI("https://gitee.com/api/v5/user?access_token=" + res.get("access_token"));
			RequestEntity<Void> requestEntity = RequestEntity.get(userInfoUri).build();
			ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(requestEntity, new ParameterizedTypeReference<>() {});
			if(!responseEntity.getStatusCode().is2xxSuccessful()) throw new RuntimeException();
			userInfoRes = responseEntity.getBody();
		} catch (Exception e){
			throw new RuntimeException(e);
		}

		if(userInfoRes == null){
			throw new RuntimeException();
		}

		List<Map<String, Object>> emailRes;
		try {
			URI userInfoUri = new URI("https://gitee.com/api/v5/emails?access_token=" + res.get("access_token"));
			RequestEntity<Void> requestEntity = RequestEntity.get(userInfoUri).build();
			ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange(requestEntity, new ParameterizedTypeReference<>() {});
			if(!responseEntity.getStatusCode().is2xxSuccessful()) throw new RuntimeException();
			emailRes = responseEntity.getBody();
		} catch (Exception e){
			throw new RuntimeException(e);
		}

		String giteeId = String.valueOf(userInfoRes.get("id"));
		String nickname = (String) userInfoRes.get("name");
		String avatar = (String) userInfoRes.get("avatar_url");
		String email = null;
		if(emailRes != null && !emailRes.isEmpty()){
			email = (String) emailRes.getFirst().get("email");
		}

		return oAuth2Prediction(new OAuth2RegisterDto(nickname, email, avatar, giteeId, "gitee"));
	}

	/**
	 * GitHub OAuth2登录获取信息
	 * @param code code
	 * @return 前端
	 */
	private OAuth2LoginVo oAuth2GithubAuthentication(String code) {
		OAuth2Properties.OAuth2Config config = oAuth2Properties.getProvider().get("github");

		Map<String, String> body = Map.of(
				"client_id", config.getClientId(),
				"client_secret", config.getClientSecret(),
				"redirect_uri", config.getRedirectUri(),
				"code", code);

		Map<String, String> res;
		try {
			URI accessTokenUrl = new URI("https://github.com/login/oauth/access_token");
			RequestEntity<Map<String, String>> requestEntity = RequestEntity.post(accessTokenUrl).body(body);
			ResponseEntity<Map<String, String>> responseEntity = restTemplate.exchange(requestEntity, new ParameterizedTypeReference<>(){});
			if(!responseEntity.getStatusCode().is2xxSuccessful()) throw new RuntimeException("github登录获取access_token响应体为空");
			res = responseEntity.getBody();
		} catch (Exception e){
			throw new RuntimeException(e);
		}

		if(res == null || res.get("access_token") == null){
			throw new RuntimeException();
		}

		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", "application/vnd.github+json");
		headers.set("Authorization", "Bearer " + res.get("access_token"));
		headers.set("X-GitHub-Api-Version", "2022-11-28");
		Map<String, Object> userInfoRes;
		try {
			URI userInfoUri = new URI("https://api.github.com/user");
			RequestEntity<Void> requestEntity = RequestEntity.get(userInfoUri)
					.headers(headers)
					.build();
			ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(requestEntity, new ParameterizedTypeReference<>() {});
			if(!responseEntity.getStatusCode().is2xxSuccessful()) throw new RuntimeException();
			userInfoRes = responseEntity.getBody();
		} catch (Exception e){
			throw new RuntimeException(e);
		}

		if(userInfoRes == null){
			throw new RuntimeException();
		}

		String githubId = String.valueOf(userInfoRes.get("id"));
		String nickname = (String) userInfoRes.get("name");
		String avatar = (String) userInfoRes.get("avatar_url");
		String email = (String) userInfoRes.get("email");

		return oAuth2Prediction(new OAuth2RegisterDto(nickname, email, avatar, githubId, "github"));
	}

	/**
	 * Google OAuth2登录获取信息
	 * @param code code
	 * @return 前端
	 */
	private OAuth2LoginVo oAuth2GoogleAuthentication(String code) {
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

		//check if login or register
		return oAuth2Prediction(new OAuth2RegisterDto(nickname, email, avatarUrlGoogle, googleId, "google"));
	}

	/**
	 * 判断是登录还是注册
	 * @param dto 用来判断的东西
	 * @return 前端返回值
	 */
	private OAuth2LoginVo oAuth2Prediction(OAuth2RegisterDto dto){
		UserOauth userOauth = userOauthService.lambdaQuery()
				.eq(UserOauth::getOwner, dto.getOwner())
				.eq(UserOauth::getOwnerId, dto.getOwnerId())
				.one();
		if(userOauth == null){
			OAuth2RegisterDto registerDto = new OAuth2RegisterDto(dto.getNickname(),
					dto.getEmail(), dto.getAvatarUrl(), dto.getOwnerId(), dto.getOwner());
			redisTemplate.opsForValue()
					.set(RedisConstant.USER_OAUTH + dto.getOwner() + ":" + dto.getOwnerId(),
							JSONUtil.toJsonStr(registerDto), RedisConstant.USER_OAUTH_TTL, TimeUnit.SECONDS);
			return new OAuth2LoginVo(false, registerDto, null);
		} else {
			User user = this.lambdaQuery()
					.eq(User::getId, userOauth.getUserId())
					.one();
			LoginVo loginVo = loginSuccess(user);
			return new OAuth2LoginVo(true, null, loginVo);
		}
	}

	@Transactional
	void oAuth2RegisterComplete(String owner, String ownerId, Long userId){
		//get avatar
		String registerDtoJson = redisTemplate.opsForValue()
				.getAndDelete(RedisConstant.USER_OAUTH + owner + ":" + ownerId);
		OAuth2RegisterDto registerDto = JSONUtil.toBean(registerDtoJson, OAuth2RegisterDto.class);

		String avatarUrlOriginal = registerDto.getAvatarUrl();
		String avatarUrl;
		String avatarFileName = UUID.randomUUID().toString();
		try {
			URL url = new URI(avatarUrlOriginal).toURL();
			URLConnection connection = url.openConnection();
			InputStream is = connection.getInputStream();
			PutObjectArgs args = PutObjectArgs.builder()
					.bucket(MinioConstant.AVATAR_BUCKET)
					.object(avatarFileName)
					.stream(is, connection.getContentLengthLong(), -1)
					.contentType("application/octet-stream")
					.build();
			minioClient.putObject(args);
			avatarUrl = MinioConstant.AVATAR_BUCKET + "/" + avatarFileName;
			is.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		//update and insert into db
		this.lambdaUpdate()
				.eq(User::getId, userId)
				.set(User::getAvatar, avatarUrl)
				.update();
		UserOauth userOauth = new UserOauth(null, userId, owner, ownerId, true, null, null);
		userOauthService.save(userOauth);
	}


}
