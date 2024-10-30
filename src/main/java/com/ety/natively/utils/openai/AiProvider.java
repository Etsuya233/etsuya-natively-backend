package com.ety.natively.utils.openai;

import lombok.Data;

/**
 * 通用OpenAI格式的提供商
 */
@Data
public class AiProvider {
	/**
	 * access token
	 */
	protected String key;
	/**
	 * 请求地址
	 */
	protected String url;
}
