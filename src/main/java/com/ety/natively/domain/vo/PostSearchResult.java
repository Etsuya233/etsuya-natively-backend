package com.ety.natively.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostSearchResult {

	private String id;

	private String title;
	private String content;
	private List<String> languages;

	private String highlightedContent;
	private LocalDateTime createTime;

}
