package com.ety.natively.config;

import com.ety.natively.domain.R;
import com.ety.natively.utils.HttpUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionsHandler {

	@PostConstruct
	public void postConstruct(){
		log.info("ExceptionsHandler已创建");
	}

//	@ExceptionHandler(BaseException.class)
//	public R<Void> handleCustomException(BaseException e){
//		log.error("自定义异常：通用：", e);
//		HttpServletResponse response = HttpUtils.getResponse();
//		if(response != null) {
//			response.setStatus(e.getStatus());
//		}
//		return R.error(e.getCode(), e.getMsg());
//	}

	@ExceptionHandler(Exception.class)
	public R<Void> handleOtherException(Exception e){
		log.error("其他异常：", e);
		return R.error("未知错误！");
	}
}
