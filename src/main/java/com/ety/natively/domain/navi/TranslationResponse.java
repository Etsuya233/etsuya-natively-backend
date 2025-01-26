package com.ety.natively.domain.navi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TranslationResponse {
	@JsonProperty(required = true, value = "first_translation")
	private String firstTranslation;
	@JsonProperty(required = true, value = "final_translation")
	private String finalTranslation;
	@JsonProperty(required = false, value = "translate_from")
	private String translateFrom;
}
