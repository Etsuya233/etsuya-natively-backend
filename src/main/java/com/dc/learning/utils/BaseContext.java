package com.dc.learning.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * 上下文
 */
@Slf4j
public class BaseContext {
	private static final ThreadLocal<Long> userIdThreadLocal = new ThreadLocal<>();

	public static Long getUserId() {
		return userIdThreadLocal.get();
	}

	public static void setUserId(Long userId) {
		userIdThreadLocal.set(userId);
	}

	public static void removeUserId() {
		userIdThreadLocal.remove();
	}
}
