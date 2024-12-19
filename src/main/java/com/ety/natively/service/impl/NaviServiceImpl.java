package com.ety.natively.service.impl;

import cn.hutool.json.JSONUtil;
import com.ety.natively.constant.NaviType;
import com.ety.natively.domain.dto.NaviRequestDto;
import com.ety.natively.domain.dto.NaviSpeakDto;
import com.ety.natively.domain.vo.NaviResult;
import com.ety.natively.properties.AzureProperties;
import com.ety.natively.service.NaviService;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.I18NUtil;
import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.PullAudioOutputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

@Service
@RequiredArgsConstructor
@Slf4j
public class NaviServiceImpl implements NaviService {

	private final ChatClient chatClient;
	private final AzureProperties azureProperties;

	@Override
	public NaviResult askNavi(NaviRequestDto dto) {
		String content = switch (dto.getType()){
			case NaviType.TRANSLATION -> askNaviTranslation(dto);
			case NaviType.EXPLANATION -> askNaviExplanation(dto);
			default -> askNaviCustom(dto);
		};
		NaviResult result = new NaviResult(content);
		return result;
	}

	@Async
	@Override
	public CompletableFuture<Void> speak(NaviSpeakDto dto, HttpServletResponse response) {
		return CompletableFuture.runAsync(() -> {
			String content = dto.getContent();

			SpeechConfig speechConfig = SpeechConfig.fromSubscription(azureProperties.getKey(), azureProperties.getRegion());
			speechConfig.setSpeechSynthesisVoiceName(this.getContentModel(content));
			speechConfig.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Ogg16Khz16BitMonoOpus);

			response.setContentType("audio/ogg");
			response.setHeader("Transfer-Encoding", "chunked");
			response.setStatus(HttpServletResponse.SC_OK);

			SpeechSynthesizer synthesizer = new SpeechSynthesizer(speechConfig, null);

			synthesizer.SynthesisStarted.addEventListener((o, e) -> {
				log.debug("Synthesizer started");
			});
			synthesizer.SynthesisCompleted.addEventListener((o, e) -> {
				log.debug("Synthesizer completed");
			});

			Future<SpeechSynthesisResult> future = synthesizer.SpeakTextAsync(content);

			try {
				ServletOutputStream os = response.getOutputStream();
				SpeechSynthesisResult result = future.get();
				long len;
				byte[] buffer = new byte[1024];
				ByteArrayInputStream stream = new ByteArrayInputStream(result.getAudioData());
				while((len = stream.read(buffer)) != -1) {
					os.write(buffer, 0, (int) len);
					os.flush();
				}
				os.close();
				log.debug("Transfer completed");
			} catch (Exception e) {
				log.error("Transfer failed", e);
			} finally {
				synthesizer.close();
				speechConfig.close();
			}
		});
	}

	private String askNaviTranslation(NaviRequestDto dto) {
		Locale language = BaseContext.getLanguage();
		Prompt prompt = TRANSLATION_SYSTEM_PROMPT_TEMPLATE.create(Map.of("language", language), TRANSLATION_OPTIONS);
		return chatClient.prompt(prompt)
				.user(dto.getQuote())
				.call()
				.content();
	}

	private String askNaviExplanation(NaviRequestDto dto) {
		Locale language = BaseContext.getLanguage();
		Prompt prompt = EXPLANATION_SYSTEM_PROMPT_TEMPLATE.create(Map.of("language", language), EXPLANATION_OPTIONS);
		return chatClient.prompt(prompt)
				.user(dto.getQuote())
				.call()
				.content();
	}

	private String askNaviCustom(NaviRequestDto dto) {
		Prompt prompt = CUSTOM_SYSTEM_PROMPT_TEMPLATE.create(CUSTOM_OPTIONS);
		String userMessage = JSONUtil.toJsonStr(Map.of("question", dto.getQuestion(), "quote", dto.getQuote()));
		return chatClient.prompt(prompt)
				.user(userMessage)
				.call()
				.content();
	}

	private String getContentModel(String content){
		Locale language = I18NUtil.getContentLanguage(content);
		if(language.equals(Locale.CHINESE)){
			return "zh-CN-YunyiMultilingualNeural";
		} else if(language.equals(Locale.JAPAN)){
			return "ja-JP-MasaruMultilingualNeural";
		} else if(language.equals(Locale.FRENCH)){
			return "fr-FR-LucienMultilingualNeural";
		} else if(language.equals(Locale.KOREAN)){
			return "ko-KR-HyunsuMultilingualNeural";
		} else {
			return "en-US-AlloyTurboMultilingualNeural";
		}
	}

	// Navi Preset
	private final String CUSTOM_SYSTEM_MESSAGE = """
			你是一位精通世界各国语言的语言学习助手，能够以母语水平与人交流。当前有人向你请教一个语言相关的问题。
		    问题由两个部分组成：一个“引用”（quote，用户引用的外语句子或短语）和一个“提问”（question，用户使用母语围绕着引用向你提问）。
		    回答请围绕着引用作答，如果引用不存在，就只需关注提问。
		    请以**提问的语言**而不是引用的语言作答，确保内容准确、清晰、完整、易于理解。如有需要请适当扩充以及举例子。

		    输入为一个 JSON 对象，包含以下两个字段：
		    question：具体的提问内容（字符串）。
		    quote：问题的引用部分（字符串）。
		    请根据输入提供答案。
			""";
	private final SystemPromptTemplate CUSTOM_SYSTEM_PROMPT_TEMPLATE = new SystemPromptTemplate(CUSTOM_SYSTEM_MESSAGE);
	private final ChatOptions CUSTOM_OPTIONS = OpenAiChatOptions.builder()
			.withModel(OpenAiApi.ChatModel.GPT_4_O_MINI)
			.build();
	private final String TRANSLATION_SYSTEM_MESSAGE = """
			你是一个资深的翻译官，能够像母语者一样讲世界上的各种语言。现在你需要将给出的原文翻译为 {language}。
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
	private final SystemPromptTemplate TRANSLATION_SYSTEM_PROMPT_TEMPLATE = new SystemPromptTemplate(TRANSLATION_SYSTEM_MESSAGE);
	private final ChatOptions TRANSLATION_OPTIONS = OpenAiChatOptions.builder()
			.withModel(OpenAiApi.ChatModel.GPT_4_O_MINI)
			.withResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, TRANSLATION_JSON_SCHEMA))
			.build();
	private final String EXPLANATION_SYSTEM_LANGUAGE = """
			你现在面对的是一个学习某门外语的学生，该学生可能看不太懂这段话。现在请你用 {language} 解释我给出的内容。主要聚焦于语言学习上。
		   	规则：
		   	1，你需要带着他分析这段话，并解释在分析过程中碰到的在语言层面上的重难点。如果文本太长（超过150个单词），你只需要分析较为重要的地方。
		   	2，内容完整，清晰，简介。不需要过多的寒暄语。
		   	你需要返回Markdown格式的内容。不要返回代码块。
			""";
	private final SystemPromptTemplate EXPLANATION_SYSTEM_PROMPT_TEMPLATE = new SystemPromptTemplate(EXPLANATION_SYSTEM_LANGUAGE);
	private final ChatOptions EXPLANATION_OPTIONS = OpenAiChatOptions.builder()
			.withModel(OpenAiApi.ChatModel.GPT_4_O_MINI)
			.build();
}
