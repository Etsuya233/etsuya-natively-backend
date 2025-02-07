package com.ety.natively.service.impl;

import com.ety.natively.constant.MessageType;
import com.ety.natively.constant.MinioConstant;
import com.ety.natively.domain.R;
import com.ety.natively.domain.dto.ClearUnreadDto;
import com.ety.natively.domain.po.*;
import com.ety.natively.domain.vo.ChatMessageVo;
import com.ety.natively.domain.vo.ConversationVo;
import com.ety.natively.domain.vo.UserVo;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.exception.BaseWSException;
import com.ety.natively.mapper.ChatMessageMapper;
import com.ety.natively.mapper.ChatUnreadMapper;
import com.ety.natively.mapper.ConversationMapper;
import com.ety.natively.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.I18NUtil;
import com.ety.natively.utils.MinioUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-12-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements IChatMessageService {

	private final SimpMessagingTemplate messagingTemplate;
	private final IChatUnreadService chatUnreadService;
	private final IConversationService conversationService;
	private final ConversationMapper conversationMapper;
	private final IUserService userService;
	private final ChatUnreadMapper chatUnreadMapper;
	private final MinioUtils minioUtils;

	private static final Object[] LOCK_POOL = new Object[1024];

	@Override
	public void sendMessage(ChatMessage message, Principal principal) {
		long userId = Long.parseLong(principal.getName());

		long senderId = Long.parseLong(principal.getName());
		long receiverId = message.getReceiverId();

		if(userId != senderId) return;

		// check if we are friend
		if(!userService.checkIsContact(senderId, receiverId)){
			throw new BaseWSException(ExceptionEnum.CHAT_NOT_CONTACT, "/queue/chat/message", receiverId);
		}

		//save
		LocalDateTime now = LocalDateTime.now();
		message.setSenderId(senderId);
		message.setCreateTime(now);
		message.setUpdateTime(now);
		this.save(message);

		//last id
		long userA = Math.min(senderId, receiverId);
		long userB = Math.max(senderId, receiverId);
		conversationMapper.saveOrUpdateLastId(userA, userB, message.getId());

		//unread (注意更新的是对方的未读！！) TODO 线程安全问题
		chatUnreadMapper.unreadCountPlusOne(senderId, receiverId);

		//send
		dispatchMessage(message);
	}

	@Override
	public void clearUnread(ClearUnreadDto dto, Principal principal) {
		long userId = Long.parseLong(principal.getName());

		chatUnreadService.lambdaUpdate()
				.set(ChatUnread::getCount, 0)
				.eq(ChatUnread::getSenderId, userId)
				.eq(ChatUnread::getReceiverId, dto.getReceiverId())
				.update();
	}

	@Override
	public List<ConversationVo> getConversationList(String lastId) {
		Long userId = BaseContext.getUserId();

		List<Conversation> conversations = conversationService.lambdaQuery()
				.lt(lastId != null, Conversation::getLastId, lastId)
				.eq(Conversation::getUserAId, userId)
				.or()
				.lt(lastId != null, Conversation::getLastId, lastId)
				.eq(Conversation::getUserBId, userId)
				.orderByDesc(Conversation::getLastId)
				.last("limit 10")
				.list();

		if(conversations.isEmpty()){
			return List.of();
		}

		List<Long> userIds = conversations.stream()
				.map(c -> c.getUserAId().equals(userId) ? c.getUserBId() : c.getUserAId())
				.toList();
		Map<Long, UserVo> userMap = userService.getUserByIds(userIds)
				.stream()
				.collect(Collectors.toMap(UserVo::getId, Function.identity()));

		List<Long> lastIds = conversations.stream()
				.map(Conversation::getLastId)
				.toList();
		Map<Long, ChatMessage> lastMsgMap = this.lambdaQuery()
				.in(ChatMessage::getId, lastIds)
				.list()
				.stream()
				.collect(Collectors.toMap(ChatMessage::getId, Function.identity()));

		Map<Long, ChatUnread> unreadMap = chatUnreadService.lambdaQuery()
				.eq(ChatUnread::getSenderId, userId)
				.in(ChatUnread::getReceiverId, userIds)
				.list()
				.stream().collect(Collectors.toMap(ChatUnread::getReceiverId, Function.identity()));

		return conversations.stream().map(c -> {
			ConversationVo vo = new ConversationVo();
			vo.setSenderId(userId);
			Long receiverId = c.getUserAId().equals(userId) ? c.getUserBId() : c.getUserAId();

			ChatUnread chatUnread = unreadMap.get(receiverId);
			if(chatUnread != null){
				vo.setUnread(chatUnread.getCount());
			} else {
				vo.setUnread(0);
			}

			UserVo user = userMap.get(receiverId);
			if(user != null){
				vo.setReceiverId(receiverId);
				vo.setNickname(user.getNickname());
				vo.setAvatar(user.getAvatar());
			}

			ChatMessage lastMsg = lastMsgMap.get(c.getLastId());
			if(lastMsg != null){
				vo.setLastId(c.getLastId());
				vo.setLastTime(lastMsg.getCreateTime());
				vo.setLastTimeDisplay(I18NUtil.getRelativeTime(lastMsg.getCreateTime()));
				vo.setContent(lastMsg.getContent());
			}

			return vo;
		})
				.filter(v -> v.getReceiverId() != null)
				.sorted((v1, v2) -> v2.getLastId().compareTo(v1.getLastId())) // larger the first TODO when last id is null it broke
				.toList();
	}

	@Override
	public List<ChatMessageVo> loadMoreOldMessage(Long userId, String lastId) {
		Long currentUserId = BaseContext.getUserId();
		List<ChatMessage> messages = this.lambdaQuery()
				.lt(lastId != null, ChatMessage::getId, lastId)
				.eq(ChatMessage::getSenderId, userId)
				.eq(ChatMessage::getReceiverId, currentUserId)
				.or()
				.lt(lastId != null, ChatMessage::getId, lastId)
				.eq(ChatMessage::getReceiverId, userId)
				.eq(ChatMessage::getSenderId, currentUserId)
				.orderByDesc(ChatMessage::getId)
				.last("limit 10")
				.list();
		return messages.stream()
				.sorted(Comparator.comparing(ChatMessage::getId))
				.map(message -> {
					if(!message.getType().equals(MessageType.TEXT)){
						message.setContent(minioUtils.generateFileUrl(MinioConstant.CHAT_ATTACHMENT_BUCKET, message.getContent()));
					}
					return ChatMessageVo.of(message);
				})
				.toList();
	}

	@Override
	public void sendFile(MultipartFile file, Integer type, Long receiverId) {
		Long senderId = BaseContext.getUserId();

		if(type <= 0 || type > 4){
			log.debug("类型不正确");
			throw new BaseException(ExceptionEnum.UNKNOWN_ERROR);
		}

		long userA = Math.min(senderId, receiverId);
		long userB = Math.max(senderId, receiverId);
		if(!userService.checkIsContact(userA, userB)){
			throw new BaseException(ExceptionEnum.CHAT_NOT_CONTACT);
		}

		String filename, url;
		try {
			filename = minioUtils.uploadFile(file, MinioConstant.CHAT_ATTACHMENT_BUCKET);
			url = minioUtils.generateFileUrl(MinioConstant.CHAT_ATTACHMENT_BUCKET, filename);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		//message
		LocalDateTime now = LocalDateTime.now();
		ChatMessage message = new ChatMessage();
		message.setSenderId(senderId);
		message.setReceiverId(receiverId);
		message.setCreateTime(now);
		message.setType(type);
		message.setContent(filename);
		this.save(message);
		//last id
		conversationMapper.saveOrUpdateLastId(userA, userB, message.getId());
		//unread (注意更新的是对方的未读！！) TODO 线程安全问题
		chatUnreadMapper.unreadCountPlusOne(senderId, receiverId);

		message.setContent(url);
		dispatchMessage(message);
	}

	private void dispatchMessage(ChatMessage message) {
		ChatMessageVo vo = ChatMessageVo.of(message);
		messagingTemplate.convertAndSendToUser(String.valueOf(message.getSenderId()),
				"/queue/chat/message",
				R.ok(vo));
		messagingTemplate.convertAndSendToUser(String.valueOf(message.getReceiverId()),
				"/queue/chat/message",
				R.ok(vo));
	}
}
