package com.dc.learning.domain.dto;

import lombok.Data;

@Data
public class AiConversationCreateDto {
	private Integer modelId;

	/**
	 * 可能为空，不为空则指定对话标题。
	 */
	private String title;
}
