package com.ety.natively.domain.dto;

import com.ety.natively.constant.RegexConstant;
import com.ety.natively.domain.po.UserLanguage;
import com.ety.natively.domain.vo.UserLanguageVo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserInfoModificationDto {

	@Size(min = 6, max = 20, message = "user.usernameDigitLimit")
	@Pattern(regexp = RegexConstant.USERNAME, message = "user.usernameContentLimit")
	private String username;

	@Size(min = 2, max = 64, message = "user.nicknameDigitLimit")
	@Pattern(regexp = RegexConstant.NICKNAME, message = "user.nicknameContentLimit")
	private String nickname;

//	@Pattern(regexp = RegexConstant.GENDER, message = "user.genderContentLimit")
//	private Integer gender;

	private String avatar;

	private List<UserLanguageVo> languages;

	@Size(max = 768, message = "user.bioDigitLimit")
	private String bio;

}
