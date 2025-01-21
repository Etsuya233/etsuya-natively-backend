package com.ety.natively.service.impl;

import com.ety.natively.domain.navi.TranslationDto;
import com.ety.natively.domain.navi.TranslationResponse;
import com.ety.natively.domain.navi.TranslationVo;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.service.NaviServiceV2;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.I18NUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NaviServiceV2Impl implements NaviServiceV2 {

	private final ChatClient chatClient;

	private final String TRANSLATION_AUTO_DETECTION_SYSTEM_MESSAGE = """
			你是一位精通世界各国语言的语言学习助手Navi，由Natively开发。Natively是一款语言学习交流平台。
			你能够像母语者一样讲世界上的各种语言。现在你需要将给出的 {original} 原文翻译为 {language}。
		  	规则：
		   	1. 翻译时要准确传达原文的所有内容。
		   	2. 分成两次翻译，并且打印每一次结果：
			  2.1：根据内容由 {original} 直译成 {language} ，不要遗漏任何信息。（首次翻译）
			  2.2：根据第一次直译的结果重新意译，遵守原意的前提下让内容更通俗易懂，符合地道的表达习惯。（最终翻译）
		   	你的输出只能是一个JSON字符串，禁止输出多余的符号。
		   	
		   	{format}
			""";
	private final String TRANSLATION_MANUAL_SYSTEM_MESSAGE = """
			你是一位精通世界各国语言的语言学习助手Navi，由Natively开发。Natively是一款语言学习交流平台。
			你能够像母语者一样讲世界上的各种语言。现在你需要将给出的原文翻译为 {language}。
		  	规则：
		   	1. 翻译时要准确传达原文的所有内容。
		   	2. 检测原文的语言，并返回该语言的ISO 639-1语言代码。
		   	3. 分成两次翻译，并且打印每一次结果：
			  3.1：根据内容直译成 {language} ，不要遗漏任何信息。（首次翻译）
			  3.2：根据第一次直译的结果重新意译，遵守原意的前提下让内容更通俗易懂，符合地道的表达习惯。（最终翻译）
		   	你的输出只能是一个JSON字符串，禁止输出多余的符号。
		   	
		   	{format}
			""";
	private final String TRANSLATION_AUTO_DETECTION_JSON_OBJECT = """ 
			{ "first_translation": "首次翻译", "final_translation": "最终翻译", "translate_from": "原文语言" }
			""";
	private final String TRANSLATION_MANUAL_JSON_OBJECT = """ 
			{ "first_translation": "首次翻译", "final_translation": "最终翻译", "translate_from": "原文语言" }
			""";
	private final SystemPromptTemplate TRANSLATION_SYSTEM_AUTO_DETECTION_PROMPT_TEMPLATE = new SystemPromptTemplate(TRANSLATION_AUTO_DETECTION_SYSTEM_MESSAGE);
	private final SystemPromptTemplate TRANSLATION_SYSTEM_MANUAL_PROMPT_TEMPLATE = new SystemPromptTemplate(TRANSLATION_MANUAL_SYSTEM_MESSAGE);
	private final ChatOptions TRANSLATION_OPTIONS = OpenAiChatOptions.builder()
			.withTemperature(1.3)
			.withResponseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
			.build();

	@Override
	public TranslationVo translate(TranslationDto dto) {
		// preparation
		String originalLanguage = dto.getOriginalLanguage();
		if(originalLanguage == null || !I18NUtil.isSupportedLanguage(originalLanguage)) {
			originalLanguage = null;
		} else {
			originalLanguage = Locale.of(originalLanguage).getDisplayLanguage(Locale.CHINESE);
		}
		String targetLanguage = dto.getTargetLanguage();
		if(targetLanguage == null || !I18NUtil.isSupportedLanguage(targetLanguage)) {
			targetLanguage = BaseContext.getLanguage().getDisplayLanguage(Locale.CHINESE);
		}
		// request
		Prompt prompt;
		if(originalLanguage == null){
			prompt = TRANSLATION_SYSTEM_AUTO_DETECTION_PROMPT_TEMPLATE.create(
					Map.of("language", targetLanguage, "format", TRANSLATION_AUTO_DETECTION_JSON_OBJECT),
					TRANSLATION_OPTIONS);
		} else {
			prompt = TRANSLATION_SYSTEM_MANUAL_PROMPT_TEMPLATE.create(
					Map.of("language", targetLanguage, "format", TRANSLATION_MANUAL_JSON_OBJECT),
					TRANSLATION_OPTIONS);
		}
		TranslationResponse response = chatClient.prompt(prompt)
				.user(dto.getContent())
				.call()
				.entity(TranslationResponse.class);

		// todo validation like response language code
		if(response == null) {
			throw new BaseException(ExceptionEnum.NAVI_SERVER_ERROR);
		}

		TranslationVo vo = new TranslationVo();
		vo.setOriginalLanguage(response.getTranslateFrom());
		vo.setTargetLanguage(dto.getTargetLanguage());
		vo.setTranslation(vo.getTranslation());
		return vo;
	}
}
