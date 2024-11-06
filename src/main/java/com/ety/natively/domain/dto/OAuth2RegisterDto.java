package com.ety.natively.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OAuth2RegisterDto {
	private String nickname;
	private String email;
	private String avatarUrl;
	private String ownerId;
	private String owner;
}
