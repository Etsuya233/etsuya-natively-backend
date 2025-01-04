package com.ety.natively.utils;

import cn.hutool.core.collection.CollUtil;
import com.ety.natively.domain.po.Language;
import com.ety.natively.domain.po.Location;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class I18NUtil {

	// -------- Basic ---------

	/**
	 * 系统自持的语言
	 */
	public static final Locale[] languages = new Locale[]{ Locale.ENGLISH, Locale.CHINESE,
			Locale.JAPANESE, Locale.FRENCH, Locale.KOREAN };
	public static final List<Language> languageInNative = new ArrayList<>();

	/**
	 * 系统支持的语言的i18n
	 */
	public static final List<Language> enLanguages = new ArrayList<>(), zhCnLanguages = new ArrayList<>(),
			jaLanguages = new ArrayList<>(), frLanguages = new ArrayList<>(),
			koLanguages = new ArrayList<>();

	/**
	 * 系统自持的语言的语言代码
	 */
	public static Set<String> languageCodes = new HashSet<>();
	static {
		for(Locale locale : languages) {
			languageCodes.add(locale.getLanguage());
			languageInNative.add(new Language(locale.getDisplayName(locale), locale.getLanguage()));
			enLanguages.add(new Language(locale.getDisplayLanguage(Locale.ENGLISH), locale.getLanguage()));
			zhCnLanguages.add(new Language(locale.getDisplayLanguage(Locale.CHINA), locale.getLanguage()));
			jaLanguages.add(new Language(locale.getDisplayLanguage(Locale.JAPAN), locale.getLanguage()));
			frLanguages.add(new Language(locale.getDisplayLanguage(Locale.FRANCE), locale.getLanguage()));
			koLanguages.add(new Language(locale.getDisplayLanguage(Locale.KOREAN), locale.getLanguage()));
		}
	}


	/**
	 * 系统自持的地区 TODO 准备Deprecated
	 */
	public static final List<Location> enLocations = new ArrayList<>(), zhCnLocations = new ArrayList<>(),
			jaLocations = new ArrayList<>(), frLocations = new ArrayList<>(),
			koLocations = new ArrayList<>();

	static {
		for (String locationCode : Locale.getISOCountries()) {
			Locale locale = Locale.of("", locationCode);
			enLocations.add(new Location(locale.getDisplayName(Locale.ENGLISH), locationCode));
			zhCnLocations.add(new Location(locale.getDisplayName(Locale.CHINA), locationCode));
			jaLocations.add(new Location(locale.getDisplayName(Locale.JAPAN), locationCode));
			frLocations.add(new Location(locale.getDisplayName(Locale.FRANCE), locationCode));
			koLocations.add(new Location(locale.getDisplayName(Locale.KOREAN), locationCode));
		}
	}

	// -------- Time ---------

	/**
	 * 调整时区
	 * @param object 对象
	 * @param fieldName 需要更改的字段名（LocalDateTime）
	 * @param <T> 对象类型
	 */
	public static <T> void adjustTimezone(T object, String fieldName){
		ZoneId timezone = BaseContext.getTimeZone();
		if(timezone == null) return;
		String capitalizedName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
		try {
			Class<?> clazz = object.getClass();
			Method getMethod = clazz.getDeclaredMethod("get" + capitalizedName);
			Method setMethod = clazz.getDeclaredMethod("set" + capitalizedName, LocalDateTime.class);
			getMethod.setAccessible(true);
			setMethod.setAccessible(true);

			LocalDateTime ret = (LocalDateTime) getMethod.invoke(object);
			LocalDateTime time = ret.atZone(ZoneOffset.UTC).withZoneSameInstant(timezone).toLocalDateTime();
			setMethod.invoke(object, time);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 调整时区
	 * @param list 列表
	 * @param fieldName 需要更改的字段名（LocalDateTime）
	 * @param <T> 类型
	 */
	public static <T> void adjustTimezone(List<T> list, String fieldName){
		if(CollUtil.isEmpty(list)) return;
		T object = list.getFirst();
		ZoneId timezone = BaseContext.getTimeZone();
		if(timezone == null) timezone = ZoneOffset.UTC;
		String capitalizedName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
		try {
			Class<?> clazz = object.getClass();
			Method getMethod = clazz.getDeclaredMethod("get" + capitalizedName);
			Method setMethod = clazz.getDeclaredMethod("set" + capitalizedName, LocalDateTime.class);
			getMethod.setAccessible(true);
			setMethod.setAccessible(true);
			for (T item : list) {
				LocalDateTime ret = (LocalDateTime) getMethod.invoke(item);
				LocalDateTime time = ret.atZone(ZoneOffset.UTC).withZoneSameInstant(timezone).toLocalDateTime();
				setMethod.invoke(item, time);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> void adjustCreateTimeTimezone(T object){
		adjustTimezone(object, "createTime");
	}

	public static <T> void adjustUpdateTimeTimezone(T object){
		adjustTimezone(object, "updateTime");
	}

	public static <T> void adjustCreateTimeTimezone(List<T> list){
		adjustTimezone(list, "createTime");
	}

	public static <T> void adjustUpdateTimeTimezone(List<T> list){
		adjustTimezone(list, "updateTime");
	}

	/**
	 * 获取i18n的相对时间
	 * @param dateTime 时间日期
	 * @return 格式化后字符串
	 */
	public static String getRelativeTime(LocalDateTime dateTime) {
		// TODO 没实现
		return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

	public static boolean isSupportedLocale(Locale locale) {
		for (Locale supportedLocale : languages) {
			if (supportedLocale.equals(locale)) {
				return true;
			}
		}
		return false;
	}

	// -------- Language Detection ---------
	private static final LanguageDetector detector = LanguageDetectorBuilder
			.fromLanguages(com.github.pemistahl.lingua.api.Language.ENGLISH, com.github.pemistahl.lingua.api.Language.CHINESE, com.github.pemistahl.lingua.api.Language.FRENCH,
					com.github.pemistahl.lingua.api.Language.JAPANESE, com.github.pemistahl.lingua.api.Language.KOREAN)
			.build();

	public static Locale getContentLanguage(String content){
		com.github.pemistahl.lingua.api.Language language = detector.detectLanguageOf(content);
		Locale ret = switch (language){
			case CHINESE -> Locale.CHINESE;
			case JAPANESE -> Locale.JAPAN;
			case KOREAN -> Locale.KOREAN;
			case FRENCH -> Locale.FRENCH;
			default -> Locale.ENGLISH;
		};
		log.debug("Detect: {}: {}", language, content);
		return ret;
	}

	// -------- Split ---------

}
