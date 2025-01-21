package com.ety.natively.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class PostCreationDto {
	private Integer type;
	private String title;
	private List<PostContentTemplate> content;
	private String verificationCode;
	private List<String> languages;
}
