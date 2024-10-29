package com.dc.learning.domain.dto;

import com.dc.learning.constant.RegexConstant;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserGeneralInfoDto {
	@NotBlank(message = "用户名不得为空")
	@Size(min = 6, max = 18, message = "用户名字数需要在6-18之间。")
	@Pattern(regexp = RegexConstant.USERNAME, message = "用户名只能包括英文字母，数字和下划线，且至少包含一位字母。")
	private String username;

	@NotBlank(message = "昵称不得为空")
	@Size(min = 2, max = 20, message = "昵称字数需要在2-20之间。")
	@Pattern(regexp = RegexConstant.NICKNAME, message = "昵称只能包括中文字符，数字，英文字母，下划线和空格。")
	private String nickname;

	@Min(value = 0, message = "请正确选择性别。")
	@Max(value = 2, message = "请正确选择性别。")
	private Integer gender;
}
