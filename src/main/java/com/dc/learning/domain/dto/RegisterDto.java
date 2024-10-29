package com.dc.learning.domain.dto;

import com.dc.learning.constant.RegexConstant;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegisterDto extends UserGeneralInfoDto{
	@NotBlank(message = "密码不得为空")
	@Size(min = 8, max = 20, message = "密码字数需要在8-20之间。")
	@Pattern(regexp = RegexConstant.PASSWORD, message = "密码需要包括大写字母，小写字母和数字。可以带特殊符号如：,.?#@$。")
	private String password;

	@Pattern(regexp = RegexConstant.PHONE, message = "只能是标准11位中国大陆手机号。")
	private String phone;
}
