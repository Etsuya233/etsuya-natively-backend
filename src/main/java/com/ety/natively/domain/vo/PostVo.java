package com.ety.natively.domain.vo;

import lombok.Data;

import java.util.List;
import java.time.LocalDateTime;

@Data
public class PostVo {
	private Long id;
	private String title;
	private String content;
	private Long userId;
	private Integer type;
	private String nickname;
	private String avatar;
	private Long upvote;
	private Long downvote;
	private Integer vote;
	private Long commentCount;
	private LocalDateTime createTime;
	private List<AttachmentVo> images;
	private AttachmentVo voice;
}
