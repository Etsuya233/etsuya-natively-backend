package com.ety.natively.config;

import com.ety.natively.utils.TranslationUtil;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

@Configuration
public class I18nConfig {

	@Bean
	public MessageSource messageSource() {
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();

		source.setBasename("lang/lang");
		source.setDefaultEncoding("UTF-8");
		source.setDefaultLocale(Locale.ENGLISH);
		source.setFallbackToSystemLocale(false);

		return source;
	}

	@Bean
	public TranslationUtil t(){
		return new TranslationUtil(messageSource());
	}
}
