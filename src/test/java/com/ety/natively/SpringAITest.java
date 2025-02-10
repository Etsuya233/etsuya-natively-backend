package com.ety.natively;

import com.ety.natively.domain.vo.NaviResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SpringAITest {

	@Autowired
	public ChatClient chatClient;

	private static final String POST_AI_SYSTEM_MESSAGE = """
			你是Navi AI，由Natively制作的AI。Natively是一款语言学习交流平台。请根据用户帖子的内容生成回复，要求如下：
			如果帖子内容为问题，直接回答问题。
			如果帖子内容是一篇文章，请对文章进行总结，并突出其亮点。
			如果帖子内容属于其他类型，请根据内容生成合适的回复。
			请使用与用户帖子相同的语言回答。
			回复须为纯文本格式，不使用Markdown语法。
			回复语气要自然、亲切，仿佛与用户面对面交流。
			回复不要太过于长。
			请严格按照以上规则生成符合要求的回复。
			""";

	private static final String QUESTION = "今天学到了标日的40课。学到了三种ところ的用法。但是这个いるところです有点困惑。。。\n" +
			"「電車を乗っているところです」和「電車を乗っているです」有什么区别呢？？？";

	@Test
	public void test(){
		// 这个东西可以被多次使用，每次都会添加历史记录
		InMemoryChatMemory memory = new InMemoryChatMemory();

		String content1 = chatClient.prompt()
				.user("電車を乗っているです是什么意思？")
				.options(ChatOptions.builder()
						.model("deepseek-chat")
						.maxTokens(4096)
						.temperature(1.3)
						.build())
				.advisors(new MessageChatMemoryAdvisor(memory))
				.call()
				.content();

		System.out.println(content1);

		String content2 = chatClient.prompt()
				.user("那電車を乗っているところです又是什么意思？")
				.options(ChatOptions.builder()
						.model("deepseek-chat")
						.maxTokens(4096)
						.temperature(1.3)
						.build())
				.advisors(new MessageChatMemoryAdvisor(memory))
				.call()
				.content();

		System.out.println(content2);

	}

}
