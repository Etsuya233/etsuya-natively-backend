package com.ety.natively.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVoV2 {
	private Long id;
	private Long parentId;
	private Boolean post;

	private String nickname;
	private Long userId;
	private List<UserLanguageVo> userLanguages;
	private String avatar;

	private Long upvote = 0L;
	private Long downvote = 0L;
	private Integer vote;
	private Long commentCount = 0L;

	private String content;
	private String image;
	private String voice;
	private String compare;

	private LocalDateTime createTime;
}
