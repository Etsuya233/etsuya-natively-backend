package com.ety.natively.domain.dto;

import lombok.Data;

@Data
public class BookmarkUpdateDto {
	private Long id;
	private String content;
	private String note;
}
