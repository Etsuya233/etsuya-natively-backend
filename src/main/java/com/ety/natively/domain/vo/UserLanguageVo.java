package com.ety.natively.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLanguageVo {
	private String language;
	private Integer proficiency;
}
