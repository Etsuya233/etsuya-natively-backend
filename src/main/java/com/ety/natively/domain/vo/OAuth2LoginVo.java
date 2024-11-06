package com.ety.natively.domain.vo;

import com.ety.natively.domain.dto.OAuth2RegisterDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2LoginVo {
	private Boolean login;
	private OAuth2RegisterDto registerInfo;
	private LoginVo loginInfo;
}
