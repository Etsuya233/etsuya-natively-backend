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
import com.ety.natively.service.IPostService;
import com.ety.natively.service.IPostSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
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
	private final IPostService postService;
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
	public void addCommentCount(Long commentId){
		// cascade comment count
		postMapper.addCommentCount(commentId);

		// post comment count
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
			你需要根据用户提问的语言回复。如果使用英语提问就使用英语回答。（很重要的一点！）
			回复须为纯文本格式，可以使用换行符等。禁止使用任何Markdown语法，包括加粗等。
			回复语气要自然、亲切，仿佛与用户面对面交流。
			回复不要太过于长。
			请严格按照以上规则生成符合要求的回复。后续会给出用户内容。
			
			用户帖子如下：
			
			""";
	public static final ChatOptions POST_AI_OPTIONS = ChatOptions.builder()
			.model("deepseek-r1")
			.build();

	@Bean
	public RabbitListenerContainerFactory<SimpleMessageListenerContainer> postAiContainerFactory(ConnectionFactory connectionFactory){
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setPrefetchCount(1);
		factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
		return factory;
	}

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
				.user(POST_AI_SYSTEM_MESSAGE + content)
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

		CommentSummary summary = new CommentSummary();
		summary.setCommentId(comment.getId());
		commentSummaryService.save(summary);

		postSummaryService.lambdaUpdate()
				.eq(PostSummary::getPostId, postId)
				.setIncrBy(PostSummary::getCommentCount, 1)
				.update();
	}

}
