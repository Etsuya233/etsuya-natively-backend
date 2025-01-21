package com.ety.natively.domain.dto;

import com.ety.natively.constant.PostContentType;
import lombok.Data;

@Data
public class PostContentText implements PostContentTemplate {
	private Integer type = PostContentType.TEXT;
	private String value;
}
