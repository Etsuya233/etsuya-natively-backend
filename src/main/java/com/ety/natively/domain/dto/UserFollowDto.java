package com.ety.natively.domain.dto;

import lombok.Data;

@Data
public class UserFollowDto {
	private Long followeeId;
	private Boolean follow;
}