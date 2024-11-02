package com.ety.natively.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

public class TranslationUtil {

	private static MessageSource source;

	public TranslationUtil(MessageSource messageSource){
		source = messageSource;
	}

	public String get(String key, Locale locale, String... args){
		try {
			return source.getMessage(key, args, locale);
		} catch (NoSuchMessageException e) {
			return "";
		}
	}

	public String get(String key, String... args){
		return get(key, BaseContext.getLanguage(), args);
	}

}
