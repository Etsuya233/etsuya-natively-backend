package com.ety.natively.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

public class TranslationUtil {

	private static MessageSource source;

	public TranslationUtil(MessageSource messageSource){
		source = messageSource;
	}

	public String get(String key, Locale locale, Object... args){
		try {
			return source.getMessage(key, args, locale);
		} catch (NoSuchMessageException e) {
			return "";
		}
	}

	public String get(String key, Object... args){
		return get(key, BaseContext.getLanguage(), args);
	}

}
