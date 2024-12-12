package com.ety.natively.domain.dto;

import com.ety.natively.domain.vo.ExplanationVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExplanationDto {
	private Integer originType; //1 帖子 2 评论 3 消息 4 自动生成
	private Long referenceId;


}
