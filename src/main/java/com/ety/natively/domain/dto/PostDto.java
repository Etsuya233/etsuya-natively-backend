package com.ety.natively.domain.dto;

import lombok.Data;

@Data
public class PostDto {
	private String title;
	private String content;
	/**
	 * 普通1，问答2
	 */
	private Integer type;
}
