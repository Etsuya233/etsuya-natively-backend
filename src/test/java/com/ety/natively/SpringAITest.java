package com.ety.natively;

import com.ety.natively.domain.ai.TranslationAiResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest
public class SpringAITest {

	private final ChatClient chatClient;

	public SpringAITest(@Autowired ChatClient.Builder builder) {
		this.chatClient = builder.build();
	}

	String systemMsg = """
			你是一个资深的翻译官，能够像母语者一样讲世界上的各种语言。现在你需要将 {from} 翻译为 {to} 。
			规则：
			1，翻译时要准确传达新闻事实和背景。
			2，分成两次翻译，并且打印每一次结果：
				2.1：根据内容直译，不要遗漏任何信息。（首次翻译）
				2.2：根据第一次直译的结果重新意译，遵守原意的前提下让内容更通俗易懂，符合地道的表达习惯。（最终翻译）
			你的输出是一个JSON，表格式会给出。
			""";
	String jsonSchema = """
			{
			  "name": "TranslationOutput",
			  "description": "Schema for validating translation output from the translation task.",
			  "type": "object",
			  "properties": {
			    "first_translation": {
			      "type": "string",
			      "description": "The direct translation of the input content, conveying all information accurately."
			    },
			    "final_translation": {
			      "type": "string",
			      "description": "The paraphrased translation of the first translation, ensuring natural and idiomatic expression while retaining the original meaning."
			    }
			  },
			  "required": ["first_translation", "final_translation"],
			  "additionalProperties": false
			}
			""";
	String userMsg = """
			Mr Mangione is in jail in Pennsylvania, where he was formally charged with possession of an unlicensed firearm, forgery and providing false identification to police.
			He was handcuffed at the wrists and ankles when he appeared in court there earlier on Monday.
			Wearing jeans and a dark blue jersey, Mr Mangione seemed calm during the hearing, occasionally looking around at those present, including the media.
			Last week's shooting triggered a huge manhunt, with New York City investigators using one of the world's largest digital surveillance systems as well as police dogs, drones and divers in a Central Park lake to search for the attacker.
			Investigators revealed that finding Mr Mangione was a complete surprise, as they did not have his name on a list of suspects before Monday.
			""";

	@Test
	public void testFirst(){
		SystemPromptTemplate promptTemplate = new SystemPromptTemplate(systemMsg);
		Prompt prompt = promptTemplate.create(Map.of("from", "英语", "to", "日语"), OpenAiChatOptions.builder()
				.withModel("gpt-4o-mini")
				.withResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, jsonSchema)).build());
		TranslationAiResult result = chatClient.prompt(prompt)
				.user(userMsg)
				.call()
				.entity(TranslationAiResult.class);
		System.out.println(result);
	}
}
