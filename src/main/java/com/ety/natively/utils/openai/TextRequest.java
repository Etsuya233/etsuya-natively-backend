package com.ety.natively.utils.openai;

import com.dc.learning.domain.po.AiModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文字AI请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextRequest {
	private String model;
	private List<Message> messages;
	private Double temperature;
	@JsonProperty("top_p")
	private Double topP;
	@JsonProperty("max_tokens")
	private Integer maxTokens;
	private Boolean stream;
	private Integer n;
	@JsonProperty("presence_penalty")
	private Double presencePenalty;
	@JsonProperty("frequency_penalty")
	private Double frequencyPenalty;
	private String user;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Message {
		private String role;
		private String content;
	}

	public static TextRequest of(AiModel model, List<Message> messages){
		return new TextRequest(model.getModelName(), messages, model.getTemperature(), model.getTopP(), model.getMaxTokens(),
				false, null, model.getPresencePenalty(), model.getFrequencyPenalty(), null);
	}
}
