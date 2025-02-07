package com.ety.natively.domain.dto;

import lombok.Data;

@Data
public class SearchDto {
	private String content;
	private String mode;
	private Integer from;
}
