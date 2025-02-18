package com.ety.natively.domain.elastic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDocument {
	private Long id;
	private String username;
	private String nickname;
}
