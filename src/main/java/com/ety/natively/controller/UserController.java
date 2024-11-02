package com.ety.natively.controller;


import com.ety.natively.domain.R;
import com.ety.natively.domain.dto.LoginDto;
import com.ety.natively.domain.dto.RegisterDto;
import com.ety.natively.domain.dto.UserInfoModificationDto;
import com.ety.natively.domain.dto.UserRefreshDto;
import com.ety.natively.domain.po.User;
import com.ety.natively.domain.vo.LoginVo;
import com.ety.natively.service.IUserService;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.TranslationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
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

	@RequestMapping("/hello")
	public R<String> helloRequest(){
		return R.ok(ZonedDateTime.now()
				.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(BaseContext.getLanguage())));
	}

}
