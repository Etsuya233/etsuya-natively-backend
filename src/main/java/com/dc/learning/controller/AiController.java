package com.dc.learning.controller;

import com.dc.learning.domain.R;
import com.dc.learning.domain.dto.AiDto;
import com.dc.learning.domain.dto.AiRecordDto;
import com.dc.learning.domain.dto.ChangeNameDto;
import com.dc.learning.domain.dto.TestAiDto;
import com.dc.learning.domain.po.AiRecord;
import com.dc.learning.domain.vo.AiRecordWithTitle;
import com.dc.learning.domain.vo.ConversationVo;
import com.dc.learning.domain.vo.ModelVo;
import com.dc.learning.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

	private final AiService aiService;

	@GetMapping("/model")
	public R<List<ModelVo>> getModelList(){
		List<ModelVo> ret = aiService.getAllModelList();
		return R.ok(ret);
	}

	@PostMapping
	public R<AiRecordWithTitle> getResultFromAi(@RequestBody AiDto aiDto){
		AiRecordWithTitle ret = aiService.getResultFromAi(aiDto);
		return R.ok(ret);
	}

	@GetMapping("/conversation")
	public R<List<ConversationVo>> getAllConversation(){
		return R.ok(aiService.getAllConversations());
	}

	@PostMapping("/conversation")
	public R<ConversationVo> createConversation(@RequestBody Integer modelId){
		ConversationVo conversation = aiService.createConversation(modelId);
		return R.ok(conversation);
	}

	@PostMapping("/record")
	public R<List<AiRecord>> getMoreRecord(@RequestBody AiRecordDto dto){
		List<AiRecord> ret = aiService.getCharRecord(dto);
		return R.ok(ret);
	}

	@DeleteMapping
	public R<Void> deleteConversation(@RequestBody Long id){
		aiService.deleteConversation(id);
		return R.ok();
	}

	@PostMapping("/name")
	public R<Void> changeConversationName(@RequestBody ChangeNameDto dto){
		aiService.changeName(dto);
		return R.ok();
	}

}
