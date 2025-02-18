package com.ety.natively.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ety.natively.domain.dto.*;
import com.ety.natively.domain.po.User;
import com.ety.natively.domain.vo.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

	LoginVo register(RegisterDto registerDto);

	LoginVo refreshUserToken(UserRefreshDto dto);

	UserVo getCurrent();

	UserVo getUserInfo(Long id);

	UserVo getUserInfoWithExtra(Long id);

	Boolean usernameUnique(String username);

	Boolean emailUnique(String email);

	void modifyUserInfo(UserInfoModificationDto dto);

	OAuth2LoginVo oAuth2Login(OAuth2Request request);

	List<UserVo> getUserByIds(List<Long> ids);

	List<UserVo> getContacts(Long lastId);

	boolean checkIsContact(long senderId, long receiverId);

	FollowVo follow(UserFollowDto dto);

	List<UserLinkedAccountVo> getUserLinkedAccounts();

	void oAuth2Unlink(OAuth2UnlinkDto request);

	List<UserVo> getFollowing(Long userId, Long lastId);

	List<UserVo> getFollowers(Long userId, Long lastId);

	String uploadAvatar(MultipartFile avatar);

	void changePassword(ChangePasswordDto dto);

//	OAuth2LoginVo oAuth2Link(OAuth2Request request);
}
