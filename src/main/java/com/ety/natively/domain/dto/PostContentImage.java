package com.ety.natively.domain.dto;

import com.ety.natively.constant.PostContentType;
import lombok.Data;

@Data
public class PostContentImage implements PostContentTemplate {
	private Integer type = PostContentType.IMAGE;
	private String name;
	private String caption;
}
