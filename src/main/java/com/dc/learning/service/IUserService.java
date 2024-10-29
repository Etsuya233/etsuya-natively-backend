package com.dc.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dc.learning.domain.dto.LoginDto;
import com.dc.learning.domain.dto.RegisterDto;
import com.dc.learning.domain.dto.UserGeneralInfoDto;
import com.dc.learning.domain.vo.LoginVo;

import com.dc.learning.domain.po.User;
import jakarta.validation.Valid;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-09-26
 */
public interface IUserService extends IService<User> {

	LoginVo login(LoginDto loginDto);

	void register(@Valid RegisterDto registerDto);

	User getCurrent();

	User getUserInfo(Long id);

	String usernameUnique(String username);

	void modifyUserInfo(UserGeneralInfoDto dto);

	String phoneUnique(String phone);

	LoginVo refreshUserToken(String refreshToken);
}
