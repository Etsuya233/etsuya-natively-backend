package com.ety.natively.domain.dto;

import com.ety.natively.constant.RegexConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.util.List;

@Data
public class RegisterDto {

	@NotBlank(message = "user.usernameNotBlank")
	@Size(min = 6, max = 20, message = "user.usernameDigitLimit")
	@Pattern(regexp = RegexConstant.USERNAME, message = "user.usernameContentLimit")
	private String username;

	@NotBlank(message = "user.nicknameNotBlank")
	@Size(min = 2, max = 64, message = "user.nicknameDigitLimit")
	@Pattern(regexp = RegexConstant.NICKNAME, message = "user.nicknameContentLimit")
	private String nickname;

	@NotBlank(message = "user.passwordNotBlank")
	@Size(min = 6, max = 20, message = "user.passwordDigitLimit")
	@Pattern(regexp = RegexConstant.PASSWORD, message = "user.passwordContentLimit")
	private String password;

	@Size(max = 255, message = "user.emailDigitLimit")
	@Pattern(regexp = "^$|" + RegexConstant.EMAIL, message = "user.emailContentLimit")
	private String email;

	@Range(min = 0, max = 2, message = "user.genderContentLimit")
	private Integer gender;

	@Size(max = 32, message = "user.timezoneDigitLimit")
	private String timezone;

	private List<LanguageSelection> language;

	@Data
	public static class LanguageSelection {
		private String language;
		private Integer proficiency;
	}

	private String owner;
	private String ownerId;
}

