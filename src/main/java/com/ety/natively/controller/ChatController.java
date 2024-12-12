package com.ety.natively.controller;


import com.ety.natively.domain.R;
import com.ety.natively.domain.dto.ClearUnreadDto;
import com.ety.natively.domain.po.ChatMessage;
import com.ety.natively.domain.vo.ChatMessageVo;
import com.ety.natively.domain.vo.ConversationVo;
import com.ety.natively.domain.vo.LookUpVo;
import com.ety.natively.service.IChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-12-04
 */
@RestController
@RequestMapping("/chat")
@Slf4j
@RequiredArgsConstructor
public class ChatController {

	private final SimpMessagingTemplate messagingTemplate;
	private final IChatMessageService chatMessageService;

	@MessageMapping("/msg")
	public void sendMessage(ChatMessage message, Principal principal) {
		log.debug("Received: {}", message);
		chatMessageService.sendMessage(message, principal);
	}

	@MessageMapping("/clear")
	public void clearUnread(ClearUnreadDto dto, Principal principal) {
		log.debug("Received: {}", dto);
		chatMessageService.clearUnread(dto, principal);
	}

	@GetMapping("/list")
	public R<List<ConversationVo>> getConversationList(@RequestParam(required = false) String lastId){
		List<ConversationVo> ret = chatMessageService.getConversationList(lastId);
		return R.ok(ret);
	}

	@GetMapping("/msg")
	public R<List<ChatMessageVo>> loadMoreOldMessage(@RequestParam("userId") Long userId,
													 @RequestParam(required = false) String lastId){
		List<ChatMessageVo> ret = chatMessageService.loadMoreOldMessage(userId, lastId);
		return R.ok(ret);
	}

}
