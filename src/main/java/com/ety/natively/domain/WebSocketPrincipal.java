package com.ety.natively.domain;

import lombok.Data;

import java.security.Principal;
import java.util.Locale;

@Data
public class WebSocketPrincipal implements Principal {
	private String name;
	private Locale language;
	private Long userId;
}
