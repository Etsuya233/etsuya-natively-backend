package com.ety.natively.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ConversationVo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private String nickname;

	private Long senderId;

	private Long receiverId;

	private String avatar;

	private Integer unread;

	private String content;

	private String lastTimeDisplay; //相对时间

	private LocalDateTime lastTime; //日期标准

	private Long lastId;
}
