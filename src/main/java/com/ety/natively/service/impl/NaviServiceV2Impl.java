package com.ety.natively.service.impl;

import cn.hutool.json.JSONUtil;
import cn.hutool.system.RuntimeInfo;
import com.ety.natively.domain.R;
import com.ety.natively.domain.WebSocketPrincipal;
import com.ety.natively.domain.navi.AskStreamDto;
import com.ety.natively.domain.navi.*;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.properties.AzureProperties;
import com.ety.natively.service.NaviServiceV2;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.I18NUtil;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
@Slf4j
public class NaviServiceV2Impl implements NaviServiceV2 {

	private final ChatClient chatClient;
	private final AzureProperties azureProperties;
	private final StringRedisTemplate redisTemplate;
	private final SimpMessagingTemplate messagingTemplate;

	private final String TRANSLATION_MANUAL_SYSTEM_MESSAGE = """
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
	private final String TRANSLATION_AUTO_DETECTION_SYSTEM_MESSAGE = """
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
			.temperature(1.3)
			.responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
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
					Map.of("language", targetLanguage, "format", TRANSLATION_MANUAL_JSON_OBJECT, "original", originalLanguage),
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
		vo.setTranslation(response.getFinalTranslation());
		return vo;
	}

	// Navi Preset
	private final String ASK_SYSTEM_MESSAGE = """
			你是一位精通世界各国语言的语言学习助手Navi，由Natively开发。Natively是一款语言学习交流平台。
			你能够以母语水平与人交流。当前有人向你请教一个语言相关的问题。
		    问题由两个部分组成：一个“引用”（quote，用户引用的外语句子或短语）和一个“提问”（question，用户使用母语围绕着引用向你提问）。
		    回答请围绕着引用作答，如果引用不存在，就只需关注提问。
		    请以**提问的语言**而不是引用的语言作答，确保内容准确、清晰、完整、易于理解。如有需要请适当扩充以及举例子。

		    输入为一个 JSON 对象，包含以下两个字段：
		    question：具体的提问内容（字符串）。
		    quote：问题的引用部分（字符串）。
		    
		    请根据输入提供答案。
			""";
	private final SystemPromptTemplate ASK_SYSTEM_PROMPT_TEMPLATE = new SystemPromptTemplate(ASK_SYSTEM_MESSAGE);
	private final ChatOptions ASK_OPTIONS = OpenAiChatOptions.builder()
			.temperature(1.3)
			.build();

	@Override
	public AskVo ask(AskDto dto) {
		String question = dto.getQuestion();
		String quote = dto.getQuote();
		if(!StringUtils.hasText(question)){
			return new AskVo("");
		}
		if(quote == null){
			quote = "";
		}

		String userContent = JSONUtil.toJsonStr(Map.of("question", question, "quote", quote));
		Prompt prompt = ASK_SYSTEM_PROMPT_TEMPLATE.create(ASK_OPTIONS);
		String answer = chatClient.prompt(prompt)
				.user(userContent)
				.call()
				.content();

		return new AskVo(answer);
	}

	private final String EXPLANATION_SYSTEM_LANGUAGE = """
			你是一位精通世界各国语言的语言学习助手Navi，由Natively开发。Natively是一款语言学习交流平台。
			你现在面对的是一个学习某门外语的学生，该学生可能看不太懂这段话。现在请你用 {language} 解释我给出的内容。主要聚焦于语言学习上。
		   	规则：
		   	1，你需要带着他分析这段话，并解释在分析过程中碰到的在语言层面上的重难点。如果文本太长（超过150个单词），你只需要分析较为重要的地方。
		   	2，内容完整，清晰，简介。不需要过多的寒暄语。
		   	3，不要使用代码块。
		   	你需要返回Markdown格式的内容。不要返回代码块。
			""";
	private final SystemPromptTemplate EXPLANATION_SYSTEM_PROMPT_TEMPLATE = new SystemPromptTemplate(EXPLANATION_SYSTEM_LANGUAGE);
	private final ChatOptions EXPLANATION_OPTIONS = OpenAiChatOptions.builder()
			.temperature(1.0)
			.build();

	@Override
	public ExplainVo explain(ExplainDto dto) {
		String quote = dto.getQuote();
		if(!StringUtils.hasText(quote)){
			return new ExplainVo("");
		}

		Locale language = BaseContext.getLanguage();
		String languageName = language.getDisplayLanguage(Locale.CHINESE);
		Prompt prompt = EXPLANATION_SYSTEM_PROMPT_TEMPLATE.create(Map.of("language", languageName), EXPLANATION_OPTIONS);

		String answer = chatClient.prompt(prompt)
				.user(quote)
				.call()
				.content();

		return new ExplainVo(answer);
	}

	private String getModelByLanguageDetection(String content){
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

	@Override
	public CompletableFuture<Void> pronounce(PronounceDto dto, HttpServletResponse response) {
		return CompletableFuture.runAsync(() -> {
			String content = dto.getContent();

			SpeechConfig speechConfig = SpeechConfig.fromSubscription(azureProperties.getKey(), azureProperties.getRegion());
			speechConfig.setSpeechSynthesisVoiceName(this.getModelByLanguageDetection(content));
			speechConfig.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Audio16Khz32KBitRateMonoMp3);

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

	@Override
	public void askStream(AskStreamDto dto, Principal principal) {
		String question = dto.getQuestion();
		String quote = dto.getQuote();
		Long id = dto.getId();
		if(!StringUtils.hasText(question)){
			// done directly
			sendEmptyStreamResponse(id, principal.getName());
		}
		if(quote == null){
			quote = "";
		}

		String userContent = JSONUtil.toJsonStr(Map.of("question", question, "quote", quote));
		Prompt prompt = ASK_SYSTEM_PROMPT_TEMPLATE.create(ASK_OPTIONS);
		Flux<String> content = chatClient.prompt(prompt)
				.user(userContent)
				.stream()
				.content();

		// 使用MessagingTemplate返回数据
		sendStreamData(id, principal.getName(), content);
	}

	@Override
	public void explainStream(ExplainStreamDto dto, Principal principal) {
		String quote = dto.getQuote();
		Long id = dto.getId();
		if(!StringUtils.hasText(quote)){
			sendEmptyStreamResponse(id, principal.getName());
		}

		WebSocketPrincipal webSocketPrincipal = (WebSocketPrincipal) principal;
		Locale language = webSocketPrincipal.getLanguage();
		String languageName = language.getDisplayLanguage(Locale.CHINESE);
		Prompt prompt = EXPLANATION_SYSTEM_PROMPT_TEMPLATE.create(Map.of("language", languageName), EXPLANATION_OPTIONS);

		Flux<String> content = chatClient.prompt(prompt)
				.user(quote)
				.stream()
				.content();

		sendStreamData(id, principal.getName(), content);
	}

	private static final String TRANSLATION_SYSTEM_STREAM_PROMPT = """
		你是一位精通世界各国语言的语言学习助手Navi，由Natively开发。Natively是一款语言学习交流平台。
		你能够像母语者一样讲世界上的各种语言。现在你需要将给出的原文翻译为 {language}。翻译时要准确传达原文的所有内容。
		""";
	private static final SystemPromptTemplate TRANSLATION_SYSTEM_STREAM_PROMPT_TEMPLATE = new SystemPromptTemplate(TRANSLATION_SYSTEM_STREAM_PROMPT);
	private final ChatOptions TRANSLATION_STREAM_OPTIONS = OpenAiChatOptions.builder()
			.temperature(1.3)
			.build();

	@Override
	public void translateStream(TranslateStreamDto dto, Principal principal) {
		// preparation
		String targetLanguage = dto.getTargetLanguage();
		if(targetLanguage == null || !I18NUtil.isSupportedLanguage(targetLanguage)) {
			WebSocketPrincipal webSocketPrincipal = (WebSocketPrincipal) principal;
			Locale language = webSocketPrincipal.getLanguage();
			targetLanguage = language.getDisplayLanguage(Locale.CHINESE);
		} else {
			targetLanguage = Locale.of(targetLanguage).getDisplayLanguage(Locale.CHINESE);
		}
		// request
		Prompt prompt = TRANSLATION_SYSTEM_STREAM_PROMPT_TEMPLATE.create(
					Map.of("language", targetLanguage),
					TRANSLATION_STREAM_OPTIONS);

		Flux<String> content = chatClient.prompt(prompt)
				.user(dto.getContent())
				.stream()
				.content();

		sendStreamData(dto.getId(), principal.getName(), content);
	}

	private void sendEmptyStreamResponse(Long id, String userIdStr) {
		StreamVo vo = new StreamVo();
		vo.setId(id);
		vo.setStatus("done");
		messagingTemplate.convertAndSendToUser(userIdStr, "/queue/navi", R.ok(vo));
	}

	private void sendStreamData(Long id, String userIdStr, Flux<String> content) {
		content.subscribe(message -> {
			StreamVo vo = new StreamVo();
			vo.setId(id);
			vo.setStatus("message");
			vo.setMessage(message);
			messagingTemplate.convertAndSendToUser(userIdStr, "/queue/navi", R.ok(vo));
		}, error -> {
			StreamVo vo = new StreamVo();
			vo.setId(id);
			vo.setStatus("error");
			vo.setMessage(error.getMessage());
			messagingTemplate.convertAndSendToUser(userIdStr, "/queue/navi", R.ok(vo));
		}, () -> {
			sendEmptyStreamResponse(id, userIdStr);
		});
	}
}
