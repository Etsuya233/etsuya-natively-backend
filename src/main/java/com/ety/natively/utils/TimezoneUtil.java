package com.ety.natively.utils;

import cn.hutool.core.collection.CollUtil;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

public class TimezoneUtil {
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

}
