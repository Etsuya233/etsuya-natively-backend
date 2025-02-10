package com.ety.natively.controller;

import cn.hutool.json.JSONUtil;
import com.ety.natively.domain.dto.NaviReplyDto;
import com.ety.natively.listener.PostListener;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/test")
@Configuration
@RequiredArgsConstructor
public class TestController {

	private final ChatClient chatClient;

	// 使用原子计数器生成事件ID
	private final AtomicLong eventIdCounter = new AtomicLong(0);

	@GetMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> chat(@RequestParam(defaultValue = "你好") String message){
		Flux<String> content = chatClient.prompt(message)
				.stream()
				.content();
		return content
				.map(this::wrapMessageEvent)      // 包装正常消息
				.concatWith(endEventStream())     // 追加结束事件
				.onErrorResume(this::errorEvent) // 错误处理
				.onBackpressureBuffer(50);        // 背压控制
	}

	private ServerSentEvent<String> wrapMessageEvent(String content) {
		String jsonStr = JSONUtil.toJsonStr(Map.of("id", 1, "message", content));
		return ServerSentEvent.<String>builder()
				.id(String.valueOf(eventIdCounter.incrementAndGet()))
				.event("message")
				.data(jsonStr)
				.build();
	}

	// 流结束事件
	private Mono<ServerSentEvent<String>> endEventStream() {
		return Mono.just(
				ServerSentEvent.<String>builder()
						.event("done")
						.data("[DONE]")
						.build()
		);
	}

	// 错误处理事件
	private Flux<ServerSentEvent<String>> errorEvent(Throwable e) {
		return Flux.just(
				ServerSentEvent.<String>builder()
						.event("error")
						.data("Error: " + e.getMessage())
						.build()
		);
	}

	private final PostListener postListener;

	@PostMapping("/postAi")
	public void test(@RequestBody NaviReplyDto naviReplyDto){
		postListener.postReply(naviReplyDto);
	}

}
