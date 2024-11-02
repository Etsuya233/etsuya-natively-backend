package com.ety.natively.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.util.Locale;

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

	private static final ThreadLocal<Locale> languageThreadLocal = new ThreadLocal<>();

	public static Locale getLanguage() {
		return languageThreadLocal.get();
	}

	public static void setLanguage(Locale locale) {
		languageThreadLocal.set(locale);
	}

	public static void removeLanguage() {
		languageThreadLocal.remove();
	}

	public static final ThreadLocal<ZoneId> timeZoneThreadLocal = new ThreadLocal<>();

	public static ZoneId getTimeZone() {
		return timeZoneThreadLocal.get();
	}

	public static void setTimeZone(ZoneId timeZone) {
		timeZoneThreadLocal.set(timeZone);
	}

	public static void removeTimeZone() {
		timeZoneThreadLocal.remove();
	}

}
