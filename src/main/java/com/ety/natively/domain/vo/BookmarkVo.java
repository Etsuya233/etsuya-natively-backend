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
	private Long userId;
	private List<UserLanguageVo> userLanguages;
	private String nickname;
	private String avatar;
	private String note;
	private String content;
	private LocalDateTime createTime;
	private Boolean contentHasMore;
}
