package com.dc.learning.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dc.learning.constant.MqConstant;
import com.dc.learning.constant.RedisConstant;
import com.dc.learning.domain.dto.*;
import com.dc.learning.domain.po.AiConversation;
import com.dc.learning.domain.po.AiModel;
import com.dc.learning.domain.po.AiRecord;
import com.dc.learning.domain.vo.AiRecordWithTitle;
import com.dc.learning.domain.vo.ConversationVo;
import com.dc.learning.domain.vo.ModelVo;
import com.dc.learning.enums.ExceptionEnums;
import com.dc.learning.exception.BaseException;
import com.dc.learning.mapper.AiConversationMapper;
import com.dc.learning.service.AiService;
import com.dc.learning.service.IAiConversationService;
import com.dc.learning.service.IAiModelService;
import com.dc.learning.service.IAiRecordService;
import com.dc.learning.utils.BaseContext;
import com.dc.learning.utils.openai.AiProvider;
import com.dc.learning.utils.openai.TextAi;
import com.dc.learning.utils.openai.TextRequest;
import com.dc.learning.utils.openai.TextResponse;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

	@Resource(name = "apiV3Provider")
	private AiProvider aiProvider;
	private final StringRedisTemplate redisTemplate;
	private final AiConversationMapper conversationMapper;
	private final IAiModelService modelService;
	private final IAiRecordService recordService;
	private final IAiConversationService conversationService;
	private final RabbitTemplate rabbitTemplate;

	@Override
	public Long createNewConversation(AiConversationCreateDto dto){
		if(modelService.getAiModel(dto.getModelId()) == null){
			throw new BaseException("AI模型不存在。", "AI模型不存在。");
		}
		Long userId = BaseContext.getUserId();
		AiConversation conversation = new AiConversation();
		conversation.setUserId(userId);
		conversation.setModelId(dto.getModelId());
		conversation.setTitle(dto.getTitle());
		conversationService.save(conversation);
		return conversation.getId();
	}


	@Override
	@Deprecated
	public List<ConversationVo> getAllConversations(){
		LocalDateTime now = LocalDateTime.now();
		Long userId = BaseContext.getUserId();
		List<AiConversation> conversations = conversationService.lambdaQuery()
				.eq(AiConversation::getUserId, userId)
				.last("limit 10")
				.list();
		return conversations.stream()
				.map(c -> ConversationVo.of(c, now))
				.toList();
	}

	@Override
	public AiRecordWithTitle getResultFromAi(AiDto aiDto) {
		LocalDateTime now = LocalDateTime.now();
		//获取对话信息
		Long conversationId = aiDto.getConversationId();
		AiConversation conversation;
		String json = redisTemplate.opsForValue().get(RedisConstant.AI_CONVERSATION_PREFIX + conversationId);
		if(json == null){
			conversation = conversationMapper.selectById(conversationId);
			redisTemplate.opsForValue()
					.set(RedisConstant.AI_CONVERSATION_PREFIX + conversationId, JSONUtil.toJsonStr(conversation));
			redisTemplate.expire(RedisConstant.AI_CONVERSATION_PREFIX + conversationId, RedisConstant.AI_GENERAL_TTL, TimeUnit.MINUTES);
		} else {
			conversation = JSONUtil.toBean(json, AiConversation.class);
		}
		//获取模型
		AiModel model = modelService.getAiModel(conversation.getModelId());
		//添加消息 TODO 其实这里可以优化，因为携带过去的消息都是后面几个，所以Redis中维护一个对话维护一个列表，添加一条消息就删除最旧的一条消息
		List<TextRequest.Message> messages = new ArrayList<>();
		TextRequest.Message systemMessage = TextRequest.Message.builder()
				.role("system")
				.content(model.getPrompt())
				.build();
		messages.add(systemMessage);
		if(model.getAttachCount() > 0){
			List<AiRecord> records = getRecords(conversationId, null, model.getAttachCount());
			records.forEach(record -> {
				TextRequest.Message userMessage = TextRequest.Message.builder()
						.role("user")
						.content(record.getMessage())
						.build();
				TextRequest.Message assistantMessage = TextRequest.Message.builder()
						.role("assistant")
						.content(record.getResult())
						.build();
				messages.add(userMessage);
				messages.add(assistantMessage);
			});
		}
		TextRequest.Message currentMessage = TextRequest.Message.builder()
				.role("user")
				.content(aiDto.getMessage())
				.build();
		messages.add(currentMessage);
		//发送请求
		TextRequest request = TextRequest.of(model, messages);
		TextResponse response = TextAi.ai(aiProvider, request);
		//如果标题是空的则生成标题
		boolean needTitle = StrUtil.isBlank(conversation.getTitle()) && model.getGenerateTitle();
		if(needTitle){
			String title = generateTitle(aiDto.getMessage());
			conversation.setTitle(title);
		}
		//保存消息
		AiRecord record = new AiRecord();
		record.setMessage(aiDto.getMessage());
		record.setResult(response.pickOne());
		record.setCreateTime(now);
		record.setConversationId(conversationId);
		record.setUpdateTime(now);
		rabbitTemplate.convertAndSend(MqConstant.EXCHANGE.AI_TOPIC, MqConstant.KEY.AI_CONVERSATION_PERSISTENCE,
				new AiMessagePersistenceDto(conversation, record, needTitle, now));
		//返回
		record.setMessage(null);
		AiRecordWithTitle ret = new AiRecordWithTitle(record);
		if(needTitle) ret.setTitle(conversation.getTitle());
		return ret;
	}

	@Override
	public List<AiRecord> getCharRecord(AiRecordDto recordDto){
		LocalDateTime time = recordDto.getTime();
		return getRecords(recordDto.getConversationId(),
				time == null? null: LocalDateTimeUtil.toEpochMilli(time),
				5);
	}

	@Override
	public List<ModelVo> getAllModelList() {
		return modelService.getAllModels()
				.stream()
				.map(m -> new ModelVo(m.getId(), m.getName(), m.getDescription()))
				.toList();
	}

	@Override
	public ConversationVo createConversation(Integer modelId) {
		Long userId = BaseContext.getUserId();
		//检查限制
		Long count = conversationService.lambdaQuery()
				.eq(AiConversation::getUserId, userId)
				.count();
		if(count >= 10) throw new BaseException(ExceptionEnums.AI_CONVERSATION_LIMIT);
		//创建对话
		AiConversation conversation = new AiConversation();
		conversation.setModelId(modelId);
		AiModel model = modelService.getAiModel(modelId);
		if(!model.getGenerateTitle()) conversation.setTitle(model.getModelName());
		LocalDateTime now = LocalDateTime.now();
		conversation.setUserId(userId);
		conversation.setCreateTime(now);
		conversation.setUpdateTime(now);
		conversationService.save(conversation);
		//返回
		return ConversationVo.of(conversation, now);
	}

	@Override
	public void deleteConversation(Long id) {
		conversationService.removeById(id);
		rabbitTemplate.convertAndSend(MqConstant.EXCHANGE.AI_TOPIC, MqConstant.KEY.AI_CONVERSATION_DELETE);
	}

	@Override
	public void changeName(ChangeNameDto dto) {
		if(dto.getTitle().length() > 255){
			throw new BaseException("标题过长！", "标题过长！");
		}
		conversationService.update(new LambdaUpdateWrapper<AiConversation>()
				.eq(AiConversation::getId, dto.getId())
				.set(AiConversation::getTitle, dto.getTitle()));
	}

	private String generateTitle(String msg){
		TextRequest request = TextRequest.builder()
				.model("gpt-3.5-turbo")
				.messages(List.of(
						TextRequest.Message.builder()
								.role("system")
								.content("请你跟据输入内容，生成一个20个汉字或10个单词以内的标题")
								.build(),
						TextRequest.Message.builder()
								.role("user")
								.content(msg)
								.build()
				))
				.build();
		TextResponse response = TextAi.ai(aiProvider, request);
		return response.pickOne();
	}

	private List<AiRecord> getRecords(Long conversationId, Long timestamp, int count){
		if(timestamp == null) timestamp = System.currentTimeMillis();
		String redisKey = RedisConstant.AI_RECORD_PREFIX + conversationId;
		//先从Redis中找
		Set<String> recordsJson = redisTemplate.opsForZSet().reverseRangeByScore(
				redisKey, 0, timestamp, 0, count);
		List<AiRecord> records = new ArrayList<>();
		if(recordsJson != null){
			recordsJson.forEach(json -> records.add(JSONUtil.toBean(json, AiRecord.class)));
		}
		//不够的话再从MySQL找
		if(records.size() < count){
			count -= records.size();
			LocalDateTime time = records.isEmpty()? LocalDateTimeUtil.of(timestamp):
					records.get(records.size() - 1).getCreateTime();
			List<AiRecord> list = recordService.lambdaQuery()
					.eq(AiRecord::getConversationId, conversationId)
					.lt(AiRecord::getCreateTime, time)
					.orderByDesc(AiRecord::getCreateTime)
					.last("limit " + count)
					.list();
			records.addAll(list);
			//存Redis
			if(!list.isEmpty()){
				Set<ZSetOperations.TypedTuple<String>> tupleSet = list.stream()
						.map(record -> new DefaultTypedTuple<>(JSONUtil.toJsonStr(record),
								(double) LocalDateTimeUtil.toEpochMilli(record.getCreateTime())))
						.collect(Collectors.toSet());
				redisTemplate.opsForZSet().add(redisKey, tupleSet);
				redisTemplate.expire(redisKey, RedisConstant.AI_GENERAL_TTL, TimeUnit.SECONDS);
			}
		}
		//返回
		records.sort(AiRecord.compareByTimeAsc);
		return records;
	}
}
