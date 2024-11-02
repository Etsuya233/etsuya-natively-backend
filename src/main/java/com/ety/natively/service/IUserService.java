package com.ety.natively.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ety.natively.domain.dto.LoginDto;
import com.ety.natively.domain.dto.RegisterDto;
import com.ety.natively.domain.dto.UserInfoModificationDto;
import com.ety.natively.domain.dto.UserRefreshDto;
import com.ety.natively.domain.po.User;
import com.ety.natively.domain.vo.LoginVo;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-30
 */
public interface IUserService extends IService<User> {

	LoginVo login(LoginDto loginDto);

	void register(RegisterDto registerDto);

	LoginVo refreshUserToken(UserRefreshDto dto);

	User getCurrent();

	User getUserInfo(Long id);

	Boolean usernameUnique(String username);

	Boolean emailUnique(String email);

	void modifyUserInfo(UserInfoModificationDto dto);
}
