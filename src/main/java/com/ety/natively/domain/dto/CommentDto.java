package com.ety.natively.domain.dto;

import lombok.Data;

@Data
public class CommentDto {
	private Long postId;
	private Long parentId;
	private String content;
}
