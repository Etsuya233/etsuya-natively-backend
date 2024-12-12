package com.ety.natively.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookmarkVo {
	private Long id;
	private Long referenceId;
	private Integer type;
	private String title;
	private String content;
	private Long userId;
	private String nickname;
	private String avatar;
	private LocalDateTime createTime;
	private Boolean contentHasMore;
}
