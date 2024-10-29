package com.dc.learning.domain.dto;

import lombok.Data;

@Data
public class LoginDto {
	/**
	 * 可能是手机号，用户名
	 */
	private String username;
	private String password;
}
