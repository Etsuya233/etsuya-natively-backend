package com.ety.natively.domain.dto;

import lombok.Data;

@Data
public class CommentCreationDto {
	private String content;
	private String image;
	private String voice;
}
