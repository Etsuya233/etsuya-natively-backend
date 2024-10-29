package com.dc.learning.controller;


import com.dc.learning.domain.R;
import com.dc.learning.domain.dto.LoginDto;
import com.dc.learning.domain.dto.RegisterDto;
import com.dc.learning.domain.dto.UserGeneralInfoDto;
import com.dc.learning.domain.po.User;
import com.dc.learning.domain.vo.LoginVo;
import com.dc.learning.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-09-26
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

	private final IUserService userService;

	/**
	 * 密码登陆
	 * @param loginDto 手机号或用户名 密码
	 * @return 双令牌
	 */
	@PostMapping("/login")
	public R<LoginVo> login(@RequestBody LoginDto loginDto){
		LoginVo ret = userService.login(loginDto);
		return R.ok(ret);
	}

	/**
	 * 用户注册
	 * @param registerDto 注册信息
	 * @param bindingResult Validation结果
	 * @return 无
	 */
	@PostMapping("/register")
	public R<Void> register(@RequestBody @Validated RegisterDto registerDto, BindingResult bindingResult){
		if(bindingResult.hasErrors()){
			String errorMsg = bindingResult.getAllErrors().stream()
					.map(DefaultMessageSourceResolvable::getDefaultMessage)
					.collect(Collectors.joining());
			return R.error(errorMsg);
		}
		userService.register(registerDto);
		return R.ok();
	}

	@PostMapping("/refresh")
	public R<LoginVo> refreshUserToken(@RequestBody String refreshToken){
		LoginVo ret = userService.refreshUserToken(refreshToken);
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
	public R<String> usernameUnique(@RequestBody String username){
		String ret = userService.usernameUnique(username);
		return R.ok(ret);
	}

	@PostMapping("/phone")
	public R<String> phoneUnique(@RequestBody String phone){
		String ret = userService.phoneUnique(phone);
		return R.ok(ret);
	}

	@PutMapping()
	public R<Void> modifyUserInfo(@RequestBody @Validated UserGeneralInfoDto dto, BindingResult bindingResult){
		if(bindingResult.hasErrors()){
			String errorMsg = bindingResult.getAllErrors().stream()
					.map(DefaultMessageSourceResolvable::getDefaultMessage)
					.collect(Collectors.joining());
			return R.error(errorMsg);
		}
		userService.modifyUserInfo(dto);
		return R.ok();
	}


}
