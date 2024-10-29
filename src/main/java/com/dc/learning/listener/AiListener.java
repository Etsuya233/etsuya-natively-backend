package com.dc.learning.listener;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dc.learning.constant.MqConstant;
import com.dc.learning.constant.RedisConstant;
import com.dc.learning.domain.dto.AiMessagePersistenceDto;
import com.dc.learning.domain.po.AiConversation;
import com.dc.learning.domain.po.AiRecord;
import com.dc.learning.service.IAiConversationService;
import com.dc.learning.service.IAiRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiListener {

	private final StringRedisTemplate redisTemplate;
	private final IAiConversationService conversationService;
	private final IAiRecordService recordService;

	/**
	 * 删除对话的所有消息
	 * @param id 对话ID
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = MqConstant.EXCHANGE.AI_TOPIC, type = ExchangeTypes.TOPIC),
			value = @Queue(name = "ai.conversation.delete"),
			key = MqConstant.KEY.AI_CONVERSATION_DELETE
	))
	public void deleteConversationMsg(Long id){
		log.debug("删除AI对话：{}", id);
		LambdaQueryWrapper<AiRecord> queryWrapper = new LambdaQueryWrapper<AiRecord>()
				.eq(AiRecord::getConversationId, id)
				.last("limit 100");
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				boolean removed = recordService.remove(queryWrapper);
				if(!removed){
					timer.cancel();
				}
			}
		}, 0, 200);
	}

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = MqConstant.EXCHANGE.AI_TOPIC, type = ExchangeTypes.TOPIC),
			value = @Queue(name = "ai.conversation.persistence"),
			key = MqConstant.KEY.AI_CONVERSATION_PERSISTENCE
	))
	public void messagePersistence(AiMessagePersistenceDto dto){
		log.debug("持久化消息：(ConversationId){}, (Record){}", dto.getConversation().getId(), dto.getRecord().getMessage());
		AiConversation conversation = dto.getConversation();
		AiRecord record = dto.getRecord();
		Long conversationId = conversation.getId();
		if(dto.isNeedTitle()){
			conversationService.updateById(conversation);
			redisTemplate.opsForValue()
					.set(RedisConstant.AI_CONVERSATION_PREFIX + conversationId, JSONUtil.toJsonStr(conversation));
			redisTemplate.expire(RedisConstant.AI_CONVERSATION_PREFIX + conversationId, RedisConstant.AI_GENERAL_TTL, TimeUnit.MINUTES);
		}
		recordService.save(record);
		redisTemplate.opsForZSet().add(RedisConstant.AI_RECORD_PREFIX + conversationId,
				JSONUtil.toJsonStr(record), LocalDateTimeUtil.toEpochMilli(dto.getNow()));
	}

}
