package com.ety.natively.domain.navi;

import lombok.Data;

@Data
public class TranslationDto {
	private String originalLanguage;
	private String targetLanguage;
	private String content;
}
