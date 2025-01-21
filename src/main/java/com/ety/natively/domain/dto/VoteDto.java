package com.ety.natively.domain.dto;

import lombok.Data;

@Data
public class VoteDto {
	private Long id;
	/**
	 * is post or comment
	 */
	private Boolean post;
	private Integer type;
}
