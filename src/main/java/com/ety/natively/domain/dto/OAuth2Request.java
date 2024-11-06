package com.ety.natively.domain.dto;

import lombok.Data;

@Data
public class OAuth2Request {
	private String owner;
	private String code;
}
