package com.ety.natively.interceptor;

import cn.hutool.core.util.StrUtil;
import com.ety.natively.utils.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.ZoneId;
import java.util.Locale;

public class LocaleInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String localeStr = request.getHeader("Accept-Language");
		if(StrUtil.isNotBlank(localeStr)){
			Locale locale = Locale.of(localeStr);
			BaseContext.setLanguage(locale);
		} else {
			//默认英语
			BaseContext.setLanguage(Locale.ENGLISH);
		}

		String timeZoneStr = request.getHeader("Time-Zone");
		if(StrUtil.isNotBlank(timeZoneStr)){
			ZoneId zoneId = ZoneId.of(timeZoneStr);
			BaseContext.setTimeZone(zoneId);
		} else {
			//默认UTC
			BaseContext.setTimeZone(ZoneId.of("Etc/UTC"));
		}

		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		BaseContext.removeLanguage();
		BaseContext.removeTimeZone();
	}
}
