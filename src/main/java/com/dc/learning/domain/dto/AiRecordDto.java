package com.dc.learning.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiRecordDto {
	private Long conversationId;
	private LocalDateTime time;
}
