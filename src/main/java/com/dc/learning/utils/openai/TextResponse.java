package com.dc.learning.utils.openai;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 文字AI响应
 */
@Data
public class TextResponse {
	private String id;
	private String object;
	private Long created;
	private String model;
	private List<Choice> choices;
	private Usage usage;
	@JsonProperty("system_fingerprint")
	private String systemFingerprint;

	@Data
	public static class Choice {
		private int index;
		private Message message;
		private Object logprobs;
		@JsonProperty("finish_reason")
		private String finishReason;

		@Data
		public static class Message {
			private String role;
			private String content;
			private Object refusal;
		}
	}

	@Data
	public static class Usage {
		@JsonProperty("prompt_tokens")
		private int promptTokens;
		@JsonProperty("completion_tokens")
		private int completionTokens;
		@JsonProperty("total_tokens")
		private int totalTokens;
	}

	public String pickOne(){
		if(CollUtil.isNotEmpty(choices)){
			Choice.Message message = choices.get(0).getMessage();
			return message.getContent();
		}
		return null;
	}
}
