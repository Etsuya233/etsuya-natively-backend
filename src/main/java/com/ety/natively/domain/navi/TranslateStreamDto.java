package com.ety.natively.domain.navi;

import lombok.Data;

@Data
public class TranslateStreamDto {
	private String targetLanguage;
	private String content;
	private Long id;
}
