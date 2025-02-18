package com.ety.natively.controller;


import cn.hutool.json.JSONUtil;
import com.ety.natively.domain.R;
import com.ety.natively.domain.dto.*;
import com.ety.natively.domain.po.User;
import com.ety.natively.domain.vo.*;
import com.ety.natively.service.IUserService;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.TranslationUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.people.v1.model.Person;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-30
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

	private final IUserService userService;
	private final TranslationUtil t;

	@PostMapping("/login")
	public R<LoginVo> login(@RequestBody LoginDto loginDto){
		LoginVo ret = userService.login(loginDto);
		return R.ok(ret);
	}

	@PostMapping("/register")
	public R<LoginVo> register(@RequestBody @Validated RegisterDto registerDto, BindingResult bindingResult){
		if(bindingResult.hasErrors()){
			String errorMsg = bindingResult.getAllErrors().stream()
					.map(e -> t.get(e.getDefaultMessage(), BaseContext.getLanguage()))
					.collect(Collectors.joining("\n"));
			return R.error(errorMsg);
		}
		LoginVo ret = userService.register(registerDto);
		return R.ok(ret);
	}

	@PostMapping("/refresh")
	public R<LoginVo> refreshUserToken(@RequestBody UserRefreshDto dto){
		LoginVo ret = userService.refreshUserToken(dto);
		return R.ok(ret);
	}

	@GetMapping
	public R<UserVo> getCurrent(){
		UserVo user = userService.getCurrent();
		return R.ok(user);
	}

	@GetMapping("/{id}")
	public R<UserVo> getUserInfo(@PathVariable Long id){
		UserVo user = userService.getUserInfoWithExtra(id);
		return R.ok(user);
	}

	@PostMapping("/username")
	public R<Boolean> usernameUnique(@RequestBody String username){
		Boolean ret = userService.usernameUnique(username);
		return R.ok(ret);
	}

	@PostMapping("/email")
	public R<Boolean> emailUnique(@RequestBody String email){
		Boolean ret = userService.emailUnique(email);
		return R.ok(ret);
	}

	@PutMapping()
	public R<Void> modifyUserInfo(@RequestBody @Validated UserInfoModificationDto dto, BindingResult bindingResult){
		if(bindingResult.hasErrors()){
			String errorMsg = bindingResult.getAllErrors().stream()
					.map(e -> t.get(e.getDefaultMessage(), BaseContext.getLanguage()))
					.collect(Collectors.joining("\n"));
			return R.error(errorMsg);
		}
		userService.modifyUserInfo(dto);
		return R.ok();
	}

	@GetMapping("/hello")
	public R<String> helloRequest(){
		return R.ok(ZonedDateTime.now()
				.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(BaseContext.getLanguage())));
	}

	@PostMapping("/oauth2")
	public R<OAuth2LoginVo> oAuth2Login(@RequestBody OAuth2Request request){
		OAuth2LoginVo ret = userService.oAuth2Login(request);
		return R.ok(ret);
	}

	@PutMapping("/unlink")
	public R<Void> oAuth2Unlink(@RequestBody OAuth2UnlinkDto request){
		userService.oAuth2Unlink(request);
		return R.ok();
	}

	@GetMapping("/contact")
	public R<List<UserVo>> getContacts(@RequestParam(value = "lastId", required = false) Long lastId){
		List<UserVo> ret = userService.getContacts(lastId);
		return R.ok(ret);
	}

	@PostMapping("/follow")
	public R<FollowVo> follow(@RequestBody UserFollowDto dto){
		FollowVo ret = userService.follow(dto);
		return R.ok(ret);
	}

	@GetMapping("/linked")
	public R<List<UserLinkedAccountVo>> getUserLinkedAccounts(){
		List<UserLinkedAccountVo> linkedAccounts = userService.getUserLinkedAccounts();
		return R.ok(linkedAccounts);
	}

	@GetMapping("/following")
	public R<List<UserVo>> getFollowing(@RequestParam(value = "userId") Long userId,
										@RequestParam(value = "lastId", required = false) Long lastId){
		List<UserVo> ret = userService.getFollowing(userId, lastId);
		return R.ok(ret);
	}

	@GetMapping("/follower")
	public R<List<UserVo>> getFollowers(@RequestParam(value = "userId") Long userId,
										@RequestParam(value = "lastId", required = false) Long lastId){
		List<UserVo> ret = userService.getFollowers(userId, lastId);
		return R.ok(ret);
	}

	@PostMapping("/avatar")
	public R<String> uploadAvatar(@RequestParam(value = "avatar") MultipartFile avatar){
		String url = userService.uploadAvatar(avatar);
		return R.ok(url);
	}

	@PutMapping("/password")
	public R<Void> changePassword(@RequestBody ChangePasswordDto dto){
		userService.changePassword(dto);
		return R.ok();
	}

}
