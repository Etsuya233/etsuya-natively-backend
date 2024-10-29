package com.dc.learning.utils;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class HttpUtils {

	@Nullable
	public static HttpServletRequest getRequest() {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if(attributes instanceof ServletRequestAttributes){
			return ((ServletRequestAttributes) attributes).getRequest();
		}
		return null;
	}

	@Nullable
	public static HttpServletResponse getResponse() {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if(attributes instanceof ServletRequestAttributes){
			return ((ServletRequestAttributes) attributes).getResponse();
		}
		return null;
	}

}
