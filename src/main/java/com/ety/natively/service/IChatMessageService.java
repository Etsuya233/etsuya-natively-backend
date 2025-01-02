package com.ety.natively.service;

import com.ety.natively.domain.dto.ClearUnreadDto;
import com.ety.natively.domain.po.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ety.natively.domain.vo.ChatMessageVo;
import com.ety.natively.domain.vo.ConversationVo;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-12-04
 */
public interface IChatMessageService extends IService<ChatMessage> {

	void sendMessage(ChatMessage message, Principal principal);

	void clearUnread(ClearUnreadDto dto, Principal principal);

	List<ConversationVo> getConversationList(String lastId);

	List<ChatMessageVo> loadMoreOldMessage(Long userId, String lastId);

	void sendFile(MultipartFile file, Integer type, Long receiverId);
}
