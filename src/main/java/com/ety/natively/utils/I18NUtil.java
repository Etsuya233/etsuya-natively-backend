package com.ety.natively.utils;

import cn.hutool.core.collection.CollUtil;
import com.ety.natively.domain.po.Language;
import com.ety.natively.domain.po.Location;

import java.lang.reflect.Method;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class I18NUtil {

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
		Locale locale = BaseContext.getLanguage();

		// If the language is not supported, default to English
		if (!isSupportedLocale(locale)) {
			locale = Locale.ENGLISH;
		}

		// Get the current time
		LocalDateTime now = LocalDateTime.now();

		// Calculate the difference between the given time and the current time
		Duration duration = Duration.between(dateTime, now);

		// If the time is within the past hour, return minutes ago
		if (duration.toMinutes() < 60) {
			return getTimeString(duration.toMinutes(), "minute", locale);
		}

		// If the time is within the past 24 hours, return hours ago
		if (duration.toDays() < 1) {
			return getTimeString(duration.toHours(), "hour", locale);
		}

		// If the time is within the same week, return the day of the week
		if (dateTime.isAfter(now.minusWeeks(1))) {
			return getDayOfWeekString(dateTime, locale);
		}

		// Otherwise, return the full date (yyyy-MM-dd)
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return dateTime.format(formatter);
	}

	public static boolean isSupportedLocale(Locale locale) {
		for (Locale supportedLocale : languages) {
			if (supportedLocale.equals(locale)) {
				return true;
			}
		}
		return false;
	}

	private static String getTimeString(long value, String unit, Locale locale) {
		String timeUnit = unit;
		String timeStr;

		if (value == 1) {
			timeUnit = unit.substring(0, unit.length() - 1); // Singular form
		}

		timeStr = switch (locale.getLanguage()) {
			case "zh" -> value + " " + timeUnit + "前";
			case "ja" -> value + " " + timeUnit + "前";
			case "fr" -> value + " " + (unit.equals("minute") ? "minute" : "heure") + (value > 1 ? "s" : "") + " ago";
			case "ko" -> value + " " + timeUnit + "전";
			default -> // Default to English
					value + " " + timeUnit + " ago";
		};

		return timeStr;
	}

	private static String getDayOfWeekString(LocalDateTime dateTime, Locale locale) {
		DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
		String dayStr = switch (locale.getLanguage()) {
			case "zh" -> getChineseDayOfWeek(dayOfWeek);
			case "ja" -> getJapaneseDayOfWeek(dayOfWeek);
			case "fr" -> getFrenchDayOfWeek(dayOfWeek);
			case "ko" -> getKoreanDayOfWeek(dayOfWeek);
			default -> getEnglishDayOfWeek(dayOfWeek);
		};

		return dayStr;
	}

	private static String getEnglishDayOfWeek(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MONDAY -> "Monday";
			case TUESDAY -> "Tuesday";
			case WEDNESDAY -> "Wednesday";
			case THURSDAY -> "Thursday";
			case FRIDAY -> "Friday";
			case SATURDAY -> "Saturday";
			case SUNDAY -> "Sunday";
			default -> "";
		};
	}

	private static String getChineseDayOfWeek(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MONDAY -> "星期一";
			case TUESDAY -> "星期二";
			case WEDNESDAY -> "星期三";
			case THURSDAY -> "星期四";
			case FRIDAY -> "星期五";
			case SATURDAY -> "星期六";
			case SUNDAY -> "星期天";
			default -> "";
		};
	}

	private static String getJapaneseDayOfWeek(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MONDAY -> "月曜日";
			case TUESDAY -> "火曜日";
			case WEDNESDAY -> "水曜日";
			case THURSDAY -> "木曜日";
			case FRIDAY -> "金曜日";
			case SATURDAY -> "土曜日";
			case SUNDAY -> "日曜日";
			default -> "";
		};
	}

	private static String getFrenchDayOfWeek(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MONDAY -> "Lundi";
			case TUESDAY -> "Mardi";
			case WEDNESDAY -> "Mercredi";
			case THURSDAY -> "Jeudi";
			case FRIDAY -> "Vendredi";
			case SATURDAY -> "Samedi";
			case SUNDAY -> "Dimanche";
			default -> "";
		};
	}

	private static String getKoreanDayOfWeek(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MONDAY -> "월요일";
			case TUESDAY -> "화요일";
			case WEDNESDAY -> "수요일";
			case THURSDAY -> "목요일";
			case FRIDAY -> "금요일";
			case SATURDAY -> "토요일";
			case SUNDAY -> "일요일";
			default -> "";
		};
	}

}
