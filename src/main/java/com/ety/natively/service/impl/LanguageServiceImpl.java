package com.ety.natively.service.impl;

import com.ety.natively.constant.CommonType;
import com.ety.natively.domain.ai.TranslationAiResult;
import com.ety.natively.domain.dto.LookUpDto;
import com.ety.natively.domain.po.ChatMessage;
import com.ety.natively.domain.po.Comment;
import com.ety.natively.domain.po.Language;
import com.ety.natively.domain.po.Post;
import com.ety.natively.domain.vo.ExplanationVo;
import com.ety.natively.domain.vo.TranslationVo;
import com.ety.natively.mapper.ChatMessageMapper;
import com.ety.natively.mapper.CommentMapper;
import com.ety.natively.mapper.PostMapper;
import com.ety.natively.service.LanguageService;
import com.ety.natively.utils.BaseContext;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ety.natively.utils.I18NUtil.*;

@Service
@RequiredArgsConstructor
public class LanguageServiceImpl implements LanguageService {

	private final ChatClient chatClient;
	private final ChatMessageMapper chatMessageMapper;
	private final PostMapper postMapper;
	private final CommentMapper commentMapper;


	@Override
	public List<Language> getLanguages(String lang) {
		if (lang != null) lang = lang.toLowerCase();
		else lang = BaseContext.getLanguage().getLanguage();
		return switch (lang) {
			case "zh", "zh-cn" -> zhCnLanguages;
			case "ja" -> jaLanguages;
			case "fr" -> frLanguages;
			case "ko" -> koLanguages;
			default -> enLanguages;
		};
	}

	@Override
	public List<Language> getLanguageInNative() {
		return languageInNative;
	}

//	--------------- Translation -------------

	private final String TRANSLATION_SYSTEM_MESSAGE = """
			你是一个资深的翻译官，能够像母语者一样讲世界上的各种语言。现在你需要将给出的原文翻译为 {to}。
		  	规则：
		   	1. 翻译时要准确传达原文的所有内容。
		   	2. 检测原文的语言，并返回该语言的ISO 639-1语言代码。
		   	3. 分成两次翻译，并且打印每一次结果：
			  3.1：根据内容直译，不要遗漏任何信息。（首次翻译）
			  3.2：根据第一次直译的结果重新意译，遵守原意的前提下让内容更通俗易懂，符合地道的表达习惯。（最终翻译）
		   	你的输出是一个JSON，格式会在下面给出。
			""";
	private final String TRANSLATION_JSON_SCHEMA = """
			{
			  "title": "TranslationOutput",
			  "description": "Schema for validating translation output including language detection and multiple translation stages.",
			  "type": "object",
			  "properties": {
			    "first_translation": {
			      "type": "string",
			      "description": "The direct translation of the input content, conveying all information accurately."
			    },
			    "final_translation": {
			      "type": "string",
			      "description": "The paraphrased translation of the first translation, ensuring natural and idiomatic expression while retaining the original meaning."
			    },
			    "translate_from": {
			      "type": "string",
			      "description": "The detected language of the original text, represented by an ISO 639-1 language code."
			    }
			  },
			  "required": ["first_translation", "final_translation", "translate_from"],
			  "additionalProperties": false
			}
			""";
	private final SystemPromptTemplate TRANSLATION_PROMPT = new SystemPromptTemplate(TRANSLATION_SYSTEM_MESSAGE);
	private final ChatOptions TRANSLATION_OPTIONS = OpenAiChatOptions.builder()
			.withModel(OpenAiApi.ChatModel.GPT_4_O_MINI)
			.withResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, TRANSLATION_JSON_SCHEMA))
			.build();

	@Override
	public TranslationVo getTranslation(LookUpDto dto) {
		Locale userLanguage = BaseContext.getLanguage();
		String originalText = getOriginalContent(dto);
		if(originalText == null){
			return TranslationVo.empty();
		}
		String displayLanguage = userLanguage.getDisplayLanguage(Locale.CHINESE);
		Prompt prompt = TRANSLATION_PROMPT.create(Map.of("to", displayLanguage), TRANSLATION_OPTIONS);
		TranslationAiResult result = chatClient.prompt(prompt)
				.user(originalText)
				.call()
				.entity(TranslationAiResult.class);
		if(result == null){
			return TranslationVo.empty();
		}
		String translateFrom = (result.getTranslateFrom() != null)?
				Locale.of(result.getTranslateFrom()).getDisplayLanguage(userLanguage): null;
		return new TranslationVo(translateFrom, result.getFinalTranslation());
	}

	// ------------- Explanation -------------

	private final String EXPLANATION_SYSTEM_LANGUAGE = """
			你是一个优秀的外语教师，能够地道的说各门语言。
			你现在面对的是一个学习某门外语的学生，该学生可能看不太懂这段话。现在请你用 {language} 解释我给出的内容。主要聚焦于语言学习上。
			规则：你需要带着他分析这段话，并解释在分析过程中碰到的在语言层面上的重难点。如果文本太长（超过150个单词），你只需要分析较为重要的地方，最后，给出学习的建议。
			你需要返回Markdown格式的内容。
			""";
	private final SystemPromptTemplate EXPLANATION_SYSTEM_PROMPT = new SystemPromptTemplate(EXPLANATION_SYSTEM_LANGUAGE);
	private final ChatOptions EXPLANATION_OPTIONS = OpenAiChatOptions.builder()
			.withModel(OpenAiApi.ChatModel.GPT_4_O_MINI)
			.build();
	
	@Override
	public ExplanationVo getExplanation(LookUpDto dto) {
		//TODO 需要根据用户的语言水平来分析
		Locale userLanguage = BaseContext.getLanguage();
		String originalText = getOriginalContent(dto);
		if(originalText == null){
			return ExplanationVo.empty();
		}
		String displayLanguage = userLanguage.getDisplayLanguage(Locale.CHINESE);
		Prompt prompt = EXPLANATION_SYSTEM_PROMPT.create(Map.of("language", displayLanguage), EXPLANATION_OPTIONS);
		String content = chatClient.prompt(prompt)
				.user(originalText)
				.call()
				.content();
		return new ExplanationVo(content);
	}

	@Override
	public Flux<ServerSentEvent<String>> getExplanationStream(Long referenceId, Integer type, String originalText, String accessToken, String language) {
//		Locale userLanguage = BaseContext.getLanguage();
		Locale userLanguage = Locale.of(language);
		LookUpDto dto = new LookUpDto(type, referenceId, originalText);
		String text = getOriginalContent(dto);
		if (text == null) {
			return Flux.just(ServerSentEvent.builder("No content available").build());
		}
		String displayLanguage = userLanguage.getDisplayLanguage(Locale.CHINESE);
		Prompt prompt = EXPLANATION_SYSTEM_PROMPT.create(Map.of("language", displayLanguage), EXPLANATION_OPTIONS);
		Flux<String> contentFlux = chatClient.prompt(prompt)
				.user(text)
				.stream()
				.content();
		return contentFlux.map(content -> ServerSentEvent.builder(content).build());
	}


	// ------------- Utils --------------
	@Nullable
	private String getOriginalContent(LookUpDto dto) {
		return switch (dto.getType()) {
			case CommonType.POST -> {
				Post post = postMapper.selectById(dto.getReferenceId());
				yield post != null? post.getContent(): null;
			}
			case CommonType.COMMENT -> {
				Comment comment = commentMapper.selectById(dto.getReferenceId());
				yield comment != null? comment.getContent(): null;
			}
			case CommonType.MESSAGE -> {
				ChatMessage chatMessage = chatMessageMapper.selectById(dto.getReferenceId());
				yield chatMessage != null? chatMessage.getContent(): null;
			}
			default -> dto.getOriginalText();
		};
	}
}
