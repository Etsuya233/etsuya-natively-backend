package com.ety.natively.listener;

import com.ety.natively.constant.MqConstant;
import com.ety.natively.domain.dto.NaviReplyDto;
import com.ety.natively.domain.po.Comment;
import com.ety.natively.domain.po.CommentSummary;
import com.ety.natively.domain.po.PostSummary;
import com.ety.natively.mapper.CommentMapper;
import com.ety.natively.mapper.PostMapper;
import com.ety.natively.service.ICommentService;
import com.ety.natively.service.ICommentSummaryService;
import com.ety.natively.service.IPostServiceV2;
import com.ety.natively.service.IPostSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostListener {

	private final RabbitTemplate rabbitTemplate;
	private final IPostServiceV2 postService;
	private final ICommentService commentService;
	private final CommentMapper commentMapper;
	private final PostMapper postMapper;
	private final IPostSummaryService postSummaryService;
	private final ICommentSummaryService commentSummaryService;

	private final ChatClient chatClient;

	@RabbitListener(bindings = @QueueBinding(
			value = @Queue(value = MqConstant.QUEUE.POST_DELETE, durable = "true"),
			exchange = @Exchange(value = MqConstant.EXCHANGE.POST_TOPIC, type = ExchangeTypes.TOPIC, durable = "true"),
			key = MqConstant.KEY.POST_DELETE
	))
	public void deletePost(Long id){
		int loopCount = 0;
		while(loopCount < 1000){
			Integer count = postMapper.deletePostComments(id);
			if(count == 0){
				return;
			} else {
				loopCount++;
			}
		}
		log.error("帖子未完全删除: {}", id);
	}

	@RabbitListener(bindings = @QueueBinding(
			value = @Queue(value = MqConstant.QUEUE.COMMENT_DELETE, durable = "true"),
			exchange = @Exchange(value = MqConstant.EXCHANGE.POST_TOPIC, type = ExchangeTypes.TOPIC, durable = "true"),
			key = MqConstant.KEY.COMMENT_DELETE
	))
	public void deleteComment(Long id){
		List<Long> ids = postMapper.getCommentChildrenId(id);
		commentService.removeBatchByIds(ids);
		commentSummaryService.removeBatchByIds(ids);
	}

	@RabbitListener(bindings = @QueueBinding(
			value = @Queue(value = MqConstant.QUEUE.COMMENT_SCORE, durable = "true"),
			exchange = @Exchange(value = MqConstant.EXCHANGE.POST_TOPIC, type = ExchangeTypes.TOPIC, durable = "true"),
			key = MqConstant.KEY.COMMENT_SCORE
	))
	@Transactional
	public void addCommentScore(Long commentId){
		postMapper.addCommentScore(commentId);
		Comment comment = commentService.getById(commentId);
		Long postId = comment.getPostId();
		postSummaryService.lambdaUpdate()
				.eq(PostSummary::getPostId, postId)
				.setIncrBy(PostSummary::getCommentCount, 1)
				.update();
	}

	private static final String POST_AI_SYSTEM_MESSAGE = """
			你是Navi AI，由Natively制作的AI。Natively是一款语言学习交流平台。请根据用户帖子的内容生成回复，要求如下：
			如果帖子内容为问题，直接回答问题。
			如果帖子内容是一篇文章，请对文章进行总结，并突出其亮点。
			如果帖子内容属于其他类型，请根据内容生成合适的回复。
			
			注意事项：
			请使用与用户提问的语言或要求的语言回复！这个很重要！
			回复须为纯文本格式，可以使用换行符等。但禁止使用任何Markdown语法。
			回复语气要自然、亲切，仿佛与用户面对面交流。
			回复不要太过于长。
			请严格按照以上规则生成符合要求的回复。后续会给出用户内容。
			""";
	public static final ChatOptions POST_AI_OPTIONS = ChatOptions.builder()
			.model("deepseek-r1")
			.build();

	@RabbitListener(bindings = @QueueBinding(
			value = @Queue(value = MqConstant.QUEUE.POST_AI, durable = "true"),
			exchange = @Exchange(value = MqConstant.EXCHANGE.POST_TOPIC, type = ExchangeTypes.TOPIC, durable = "true"),
			key = MqConstant.KEY.POST_AI
	))
	@Transactional
	public void postReply(NaviReplyDto dto){

		//计时
		long startTime = System.currentTimeMillis();
		Long postId = dto.getPostId();
		String content = dto.getContent();

		Flux<String> contentFlux = chatClient.prompt()
				.system(POST_AI_SYSTEM_MESSAGE)
				.user(content)
				.options(POST_AI_OPTIONS)
				.stream()
				.content();

		// 将contentFlux的内容拼成一个字符串
		String reply = contentFlux.collect(StringBuilder::new, StringBuilder::append)
				.map(StringBuilder::toString)
				.block();

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;
		log.debug("AI回复耗时：{} | {} ms", postId, duration);

		if(!StringUtils.hasLength(reply)){
			throw new RuntimeException("获取AI回复失败！PostId: " + postId);
		}

		Comment comment = new Comment();

		comment.setUserId(1L);
		comment.setPostId(postId);
		comment.setParentId(postId);
		comment.setContent(reply);
		commentService.save(comment);

		commentSummaryService.lambdaUpdate()
				.eq(CommentSummary::getCommentId, comment.getId())
				.setIncrBy(CommentSummary::getCommentCount, 1)
				.update();
	}

}
