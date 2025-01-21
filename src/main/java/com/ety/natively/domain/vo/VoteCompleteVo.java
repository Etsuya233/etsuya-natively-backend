package com.ety.natively.domain.vo;

import lombok.Data;

@Data
public class VoteCompleteVo {
	private Long upvoteCount;
	private Long downvoteCount;
	private Integer vote;
}
