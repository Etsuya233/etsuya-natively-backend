package com.dc.learning.domain.dto;

import com.dc.learning.domain.po.AiConversation;
import com.dc.learning.domain.po.AiRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMessagePersistenceDto {
	private AiConversation conversation;
	private AiRecord record;
	private boolean needTitle;
	private LocalDateTime now;
}
