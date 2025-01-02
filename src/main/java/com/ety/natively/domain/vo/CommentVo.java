package com.ety.natively.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVo {
	private Long id;
	private String content;
	private String nickname;
	private Long userId;
	private List<UserLanguageVo> userLanguages;
	private Long parentId;
	private Long parentUserId;
	private String parentUserNickname;
	private String parentContent;
	private Boolean parentHasMore;
	private String avatar;
	private LocalDateTime createTime;
	private Long upvote = 0L;
	private Long downvote = 0L;
	private Integer vote;
	private Long commentCount = 0L;
	private Integer bookmarked;
	private List<AttachmentVo> images;
	private AttachmentVo voice;
}
