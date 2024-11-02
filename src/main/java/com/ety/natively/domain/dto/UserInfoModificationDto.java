package com.ety.natively.domain.dto;

import com.ety.natively.constant.RegexConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserInfoModificationDto {

	@Size(min = 6, max = 20, message = "user.usernameDigitLimit")
	@Pattern(regexp = RegexConstant.USERNAME, message = "user.usernameContentLimit")
	private String username;

	@Size(min = 2, max = 64, message = "user.nicknameDigitLimit")
	@Pattern(regexp = RegexConstant.NICKNAME, message = "user.nicknameContentLimit")
	private String nickname;

	@Pattern(regexp = RegexConstant.GENDER, message = "user.genderContentLimit")
	private Integer gender;

	@Size(max = 32, message = "user.timezoneDigitLimit")
	private String timezone;

	@Size(min = 2, max = 2, message = "user.locationDigitLimit")
	private String location;

}
