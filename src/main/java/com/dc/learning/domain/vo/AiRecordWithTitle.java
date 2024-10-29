package com.dc.learning.domain.vo;

import com.dc.learning.domain.po.AiRecord;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class AiRecordWithTitle extends AiRecord {
	private String title;

	public AiRecordWithTitle(AiRecord aiRecord){
		super(aiRecord.getId(), aiRecord.getConversationId(), aiRecord.getMessage(), aiRecord.getResult(),
				aiRecord.getFile(), aiRecord.getCreateTime(), aiRecord.getUpdateTime());
	}
}
