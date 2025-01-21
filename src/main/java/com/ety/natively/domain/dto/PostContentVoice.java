package com.ety.natively.domain.dto;

import com.ety.natively.constant.PostContentType;
import lombok.Data;

@Data
public class PostContentVoice implements PostContentTemplate {
	private Integer type = PostContentType.VOICE;
	private String name;
	private String caption;
}
