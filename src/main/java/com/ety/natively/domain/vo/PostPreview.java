package com.ety.natively.domain.vo;

import com.ety.natively.domain.po.UserLanguage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Data
public class PostPreview {
	private Long id;

	private Long userId;
	private String nickname;
	private String avatar;
	private List<UserLanguageVo> userLanguages;

	private Long upvote;
	private Long downvote;
	// 1 upvoted -1 downvoted 0 nothing
	private Integer vote;
	private Long commentCount;

	private String title;
	private String content;
	private String image;
	private String voice;
	private Boolean hasMore;
	private List<String> languages;

	// relative time
	private LocalDateTime createTime;
}
