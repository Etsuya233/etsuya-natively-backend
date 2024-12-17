package com.ety.natively.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NaviRequestDto {
	private Integer type;
	private String quote;
	private String question;
}
