package com.ety.natively.domain.dto;

import com.ety.natively.constant.PostContentType;
import lombok.Data;

@Data
public class PostContentCompare implements PostContentTemplate {
	private Integer type = PostContentType.COMPARE;
	private String oldValue;
	private String newValue;
}
