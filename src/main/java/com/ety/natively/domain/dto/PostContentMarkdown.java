package com.ety.natively.domain.dto;

import com.ety.natively.constant.PostContentType;
import lombok.Data;

@Data
public class PostContentMarkdown implements PostContentTemplate {
	private Integer type = PostContentType.MARKDOWN;
	private String value;
}
