package com.ety.natively.domain.dto;

import lombok.Data;

@Data
public class BookmarkCreateDto {
	private Integer type;
	private Long referenceId;
	private String content;
	private String note;
}
