package com.ety.natively.domain.elastic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDocument {
	private String id;
	private String title;
	private String content;
	private List<String> languages;
	private Long createTime;
}