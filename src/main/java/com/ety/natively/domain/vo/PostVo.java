package com.ety.natively.domain.vo;

import lombok.Data;

import java.util.List;
import java.time.LocalDateTime;

@Data
public class PostVo {
	private Long id;

	private String title;
	private String content;
	private String previewImage;
	private List<String> languages;

	private Long userId;
	private String nickname;
	private String avatar;
	private List<UserLanguageVo> userLanguages;

	private Long upvote;
	private Long downvote;
	private Integer vote;
	private Long commentCount;

	private Integer bookmarked;

	private LocalDateTime createTime;
}
