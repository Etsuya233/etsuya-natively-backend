package com.dc.learning.domain.vo;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.dc.learning.domain.po.AiConversation;
import com.dc.learning.utils.RelativeTimeUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVo {
	private Long id;
	private String title;
	private Integer modelId;
	private LocalDateTime createTime;
	private String relativeTime;
	private Long timestamp;

	public static ConversationVo of(AiConversation conversation, LocalDateTime now) {
		return new ConversationVo(conversation.getId(), conversation.getTitle(), conversation.getModelId(), conversation.getCreateTime(),
				RelativeTimeUtil.getRelativeTime(conversation.getCreateTime(), now), LocalDateTimeUtil.toEpochMilli(conversation.getCreateTime()));
	}
}
