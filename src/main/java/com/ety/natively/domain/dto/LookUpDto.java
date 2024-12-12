package com.ety.natively.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LookUpDto {
	private Integer type; //1 帖子 2 评论 3 消息 0 其他
	private Long referenceId;
	private String originalText;
}
