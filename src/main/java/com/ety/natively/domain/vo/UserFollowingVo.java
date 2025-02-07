package com.ety.natively.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserFollowingVo extends UserVo {
	private Long relationshipId;
}
