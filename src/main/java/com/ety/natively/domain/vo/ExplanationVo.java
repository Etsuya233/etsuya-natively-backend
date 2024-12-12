package com.ety.natively.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExplanationVo {
	private String explanation;

	public static ExplanationVo empty() {
		return new ExplanationVo("");
	}
}
