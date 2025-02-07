package com.ety.natively.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class PostSearchResult {

	private Long id;

	private Long userId;
	private Integer type;
	private String nickname;
	private String avatar;
	private List<UserLanguageVo> userLanguages;

	private String title;
	private String content;
	private List<String> lang;

	private String highlightedContent;

}
