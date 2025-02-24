package com.ety.natively.service.impl;

import cn.hutool.json.JSONUtil;
import com.ety.natively.config.StompConstant;
import com.ety.natively.domain.R;
import com.ety.natively.domain.WebSocketPrincipal;
import com.ety.natively.domain.navi.AskStreamDto;
import com.ety.natively.domain.navi.*;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.properties.AzureProperties;
import com.ety.natively.service.NaviService;
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
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class NaviServiceImpl implements NaviService {

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
		final String originalLanguageFinal = originalLanguage;
		final String targetLanguageFinal = targetLanguage;
		// request
		TranslationResponse response;
		if(originalLanguage == null){
			response = chatClient.prompt()
					.system(s -> s
							.text(TRANSLATION_AUTO_DETECTION_SYSTEM_MESSAGE)
							.param("format", TRANSLATION_AUTO_DETECTION_JSON_OBJECT).param("language", targetLanguageFinal))
					.user(dto.getContent())
					.options(TRANSLATION_OPTIONS)
					.call()
					.entity(TranslationResponse.class);
		} else {
			response = chatClient.prompt()
					.system(s -> s
							.text(TRANSLATION_MANUAL_SYSTEM_MESSAGE)
							.params(Map.of("language", targetLanguageFinal, "format", TRANSLATION_MANUAL_JSON_OBJECT, "original", originalLanguageFinal)))
					.user(dto.getContent())
					.options(TRANSLATION_OPTIONS)
					.call()
					.entity(TranslationResponse.class);
		}


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
	private final ChatOptions ASK_OPTIONS = OpenAiChatOptions.builder()
			.temperature(1.3)
			.build();

	@Override
	public AskVo ask(AskDto dto) {
		String question = dto.getQuestion();
		String quote = dto.getQuote() == null ? "" : dto.getQuote();
		if(!StringUtils.hasText(question)){
			return new AskVo("");
		}

		String userContent = JSONUtil.toJsonStr(Map.of("question", question, "quote", quote));
		String answer = chatClient.prompt()
				.system(s -> s.text(ASK_SYSTEM_MESSAGE))
				.user(userContent)
				.options(ASK_OPTIONS)
				.call()
				.content();

		return new AskVo(answer);
	}

	private final String EXPLANATION_SYSTEM_MESSAGE = """
			你是一位精通世界各国语言的语言学习助手Navi，由Natively开发。Natively是一款语言学习交流平台。
			你现在面对的是一个学习某门外语的学生，该学生可能看不太懂这段话。现在请你用 {language} 解释我给出的内容。主要聚焦于语言学习上。
		   	规则：
		   	1，你需要带着他分析这段话，并解释在分析过程中碰到的在语言层面上的重难点。如果文本太长（超过150个单词），你只需要分析较为重要的地方。
		   	2，内容完整，清晰，简介。不需要过多的寒暄语。
		   	3，不要使用代码块。
		   	你需要返回Markdown格式的内容。不要返回代码块。
			""";
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

		String answer = chatClient.prompt()
				.system(s -> s
						.text(EXPLANATION_SYSTEM_MESSAGE)
						.params(Map.of("language", languageName)))
				.user(quote)
				.options(EXPLANATION_OPTIONS)
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
		Flux<String> content = chatClient.prompt()
				.system(s -> s.text(ASK_SYSTEM_MESSAGE))
				.user(userContent)
				.options(ASK_OPTIONS)
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

		Flux<String> content = chatClient.prompt()
				.system(s -> s
						.text(EXPLANATION_SYSTEM_MESSAGE)
						.params(Map.of("language", languageName)))
				.user(quote)
				.options(EXPLANATION_OPTIONS)
				.stream()
				.content();

		sendStreamData(id, principal.getName(), content);
	}

	private static final String TRANSLATION_SYSTEM_STREAM_PROMPT = """
		你是一位精通世界各国语言的语言学习助手Navi，由Natively开发。Natively是一款语言学习交流平台。
		你能够像母语者一样讲世界上的各种语言。现在你需要将给出的原文翻译为 {language}。翻译时要准确传达原文的所有内容。
		""";
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
		final String targetLanguageFinal = targetLanguage;

		// request
		Flux<String> content = chatClient.prompt()
				.system(s -> s
						.text(TRANSLATION_SYSTEM_STREAM_PROMPT)
						.params(Map.of("language", targetLanguageFinal)))
				.user(dto.getContent())
				.options(TRANSLATION_STREAM_OPTIONS)
				.stream()
				.content();

		sendStreamData(dto.getId(), principal.getName(), content);
	}

	private void sendEmptyStreamResponse(Long id, String userIdStr) {
		StreamVo vo = new StreamVo();
		vo.setId(id);
		vo.setStatus("done");
		messagingTemplate.convertAndSendToUser(userIdStr, StompConstant.USER_NAVI_CHANNEL, R.ok(vo));
	}

	private void sendStreamData(Long id, String userIdStr, Flux<String> content) {
//		WordAccumulator accumulator = new WordAccumulator();
//		Flux<String> tokenFlux = content
//				.handle(accumulator)
//				// 流结束时，发射最后的 token（如果有）
//				.concatWith(Flux.defer(accumulator::complete))
//				// 将每次发射的 List<String> 展开成单个 String
//				.flatMapIterable(list -> list);
		content.subscribe(message -> {
			StreamVo vo = new StreamVo();
			vo.setId(id);
			vo.setStatus("message");
			vo.setMessage(message);
			messagingTemplate.convertAndSendToUser(userIdStr, StompConstant.USER_NAVI_CHANNEL, R.ok(vo));
		}, error -> {
			StreamVo vo = new StreamVo();
			vo.setId(id);
			vo.setStatus("error");
			vo.setMessage(error.getMessage());
			messagingTemplate.convertAndSendToUser(userIdStr, StompConstant.USER_NAVI_CHANNEL, R.ok(vo));
		}, () -> {
			sendEmptyStreamResponse(id, userIdStr);
		});
	}

	public static class WordAccumulator implements BiConsumer<String, SynchronousSink<List<String>>> {
		// 用于跨 chunk 累积字符
		private final StringBuilder accumulator = new StringBuilder();

		@Override
		public void accept(String chunk, SynchronousSink<List<String>> sink) {
			// 用于存储当前 chunk 处理得到的所有 token
			List<String> tokens = new ArrayList<>();
			for (char c : chunk.toCharArray()) {
				if (isDelimiter(c)) {
					// 如果遇到分隔符，先将累积的字符作为词语添加到 tokens 中
					if (accumulator.length() > 0) {
						tokens.add(accumulator.toString());
						accumulator.setLength(0);
					}
					// 分隔符本身也需要作为一个 token 添加进去
					tokens.add(String.valueOf(c));
				} else {
					accumulator.append(c);
				}
			}
			// 如果本次处理有 token，则一次性发射出去
			if (!tokens.isEmpty()) {
				sink.next(tokens);
			}
		}

		// 判断字符是否为分隔符，涵盖中文、日文、韩文、英文和法文常见标点及空白字符
		private boolean isDelimiter(char c) {
			return Character.isWhitespace(c) ||
					"，。、；：！？（）“”‘’【】《》「」『』—…·,.;!?(){}[]<>\"'\n\r\t\u3000\uFF01\uFF08\uFF09\uFF0C\uFF1A\uFF1B\uFF1F\u3001\u3002".indexOf(c) >= 0;
		}

		// 流结束时，如果累积器中还有未发射的字符，则将它们作为一个 token 发射出去
		public Flux<List<String>> complete() {
			if (accumulator.length() > 0) {
				List<String> tokens = new ArrayList<>();
				tokens.add(accumulator.toString());
				return Flux.just(tokens);
			}
			return Flux.empty();
		}
	}
}
