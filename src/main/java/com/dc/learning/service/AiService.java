package com.dc.learning.service;

import com.dc.learning.domain.dto.*;
import com.dc.learning.domain.po.AiRecord;
import com.dc.learning.domain.vo.AiRecordWithTitle;
import com.dc.learning.domain.vo.ConversationVo;
import com.dc.learning.domain.vo.ModelVo;

import java.util.List;

public interface AiService {
	@Deprecated
	Long createNewConversation(AiConversationCreateDto dto);

	List<ConversationVo> getAllConversations();

	AiRecordWithTitle getResultFromAi(AiDto aiDto);

	List<AiRecord> getCharRecord(AiRecordDto recordDto);

	List<ModelVo> getAllModelList();

	ConversationVo createConversation(Integer modelId);

	void deleteConversation(Long id);

	void changeName(ChangeNameDto dto);
}
