package com.ety.natively.domain.vo;

import com.ety.natively.domain.po.User;
import com.ety.natively.domain.po.UserLanguage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class UserVo {
	private Long id;

	private String username;

	/**
	 * 昵称
	 */
	private String nickname;


	/**
	 * 邮箱
	 */
	private String email;

	/**
	 * 性别 0 女 1 男 2 其他
	 */
	private Integer gender;

	/**
	 * IANA时区
	 */
	private String timezone;

	/**
	 * 头像
	 */
	private String avatar;

	/**
	 * 状态
	 */
	private Integer status;

	private Integer following;

	private Integer followers;

	private List<UserLanguageVo> languages;

	/**
	 * -2 对方屏蔽我 -1 我屏蔽对方 1 我关注对方 2 对方关注我 3 共同关注
	 */
	private Integer relationship;

	public static UserVo of(User user, List<UserLanguage> userLanguages){
		UserVo userVo = new UserVo();
		userVo.setId(user.getId());
		userVo.setUsername(user.getUsername());
		userVo.setNickname(user.getNickname());
		userVo.setEmail(user.getEmail());
		userVo.setGender(user.getGender());
		userVo.setTimezone(user.getTimezone());
		userVo.setAvatar(user.getAvatar());
		userVo.setStatus(user.getStatus());
		userVo.setFollowing(user.getFollowing());
		userVo.setFollowers(user.getFollowers());
		ArrayList<UserLanguageVo> langs = new ArrayList<>();
		userVo.setLanguages(langs);
		if(userLanguages != null) {
			Collections.sort(userLanguages);
			for (UserLanguage language : userLanguages) {
				langs.add(new UserLanguageVo(language.getLang(), language.getProficiency()));
			}
		}
		return userVo;
	}

	/**
	 * RelationshipStatus 接口
	 * 定义用户关系状态的常量。
	 */
	public interface RelationshipStatus {
		/**
		 * -1: 我屏蔽对方
		 */
		int BLOCKED_BY_ME = -1;

		/**
		 * -2: 对方屏蔽我
		 */
		int BLOCKED_BY_OTHER = -2;

		/**
		 * 0: 无关系
		 */
		int NO_RELATION = 0;

		/**
		 * 1: 我关注对方（单向关注）
		 */
		int ONE_WAY_FOLLOW = 1;

		/**
		 * 2: 对方关注我（单向关注）
		 */
		int ONE_WAY_FOLLOW_BY_OTHER = 2;

		/**
		 * 3: 互相关注
		 */
		int MUTUAL_FOLLOW = 3;
	}


}
