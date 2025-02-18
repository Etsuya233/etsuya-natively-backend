package com.ety.natively.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchDto {
	private String content;
	private String mode;
	private Integer from;
	private List<String> excludeLanguage;
	private Integer sort;
}
