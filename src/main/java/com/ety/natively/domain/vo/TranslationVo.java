package com.ety.natively.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationVo {
	private String translateFrom;
	private String translation;
	public static TranslationVo empty(){
		return new TranslationVo("", "");
	}
}
