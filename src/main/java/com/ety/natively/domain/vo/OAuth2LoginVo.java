package com.ety.natively.domain.vo;

import com.ety.natively.domain.dto.OAuth2Information;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2LoginVo {
	/**
	 * binding or register or login
	 */
	private String mode;
	private OAuth2Information registerInfo;
	private LoginVo loginInfo;
}
