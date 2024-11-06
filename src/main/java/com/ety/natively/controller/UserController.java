package com.ety.natively.controller;


import cn.hutool.json.JSONUtil;
import com.ety.natively.domain.R;
import com.ety.natively.domain.dto.*;
import com.ety.natively.domain.po.User;
import com.ety.natively.domain.vo.LoginVo;
import com.ety.natively.domain.vo.OAuth2LoginVo;
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
	public R<Void> register(@RequestBody @Validated RegisterDto registerDto, BindingResult bindingResult){
		if(bindingResult.hasErrors()){
			String errorMsg = bindingResult.getAllErrors().stream()
					.map(e -> t.get(e.getDefaultMessage(), BaseContext.getLanguage()))
					.collect(Collectors.joining("\n"));
			return R.error(errorMsg);
		}
		userService.register(registerDto);
		return R.ok();
	}

	@PostMapping("/refresh")
	public R<LoginVo> refreshUserToken(@RequestBody UserRefreshDto dto){
		LoginVo ret = userService.refreshUserToken(dto);
		return R.ok(ret);
	}

	@GetMapping
	public R<User> getCurrent(){
		User user = userService.getCurrent();
		return R.ok(user);
	}

	@GetMapping("/{id}")
	public R<User> getUserInfo(@PathVariable Long id){
		User user = userService.getUserInfo(id);
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

}
