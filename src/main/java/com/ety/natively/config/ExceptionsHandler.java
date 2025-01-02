package com.ety.natively.config;

import com.ety.natively.domain.R;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.HttpUtils;
import com.ety.natively.utils.TranslationUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ExceptionsHandler {

	private final TranslationUtil t;

	@PostConstruct
	public void postConstruct(){
		log.info("ExceptionsHandler已创建");
	}

	@ExceptionHandler(BaseException.class)
	public R<Void> handleCustomException(BaseException e){
		log.debug("自定义异常：通用：", e);
		ExceptionEnum exceptionEnum = e.getExceptionEnum();
		HttpServletResponse response = HttpUtils.getResponse();
		if(response != null) {
			response.setStatus(exceptionEnum.getHttpCode());
		}
		return R.error(exceptionEnum.getErrorCode(), t.get("ex." + exceptionEnum.getErrorMsgKey(), e.getExceptionArgs()));
	}

	@ExceptionHandler(Exception.class)
	public R<Void> handleOtherException(Exception e){
		log.error("其他异常：", e);
		return R.error(t.get("ex.unknown"));
	}


}
