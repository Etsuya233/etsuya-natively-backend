package com.ety.natively.config;

import com.ety.natively.domain.R;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.utils.TranslationUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.security.Principal;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class WebSocketExceptionHandler {

	private final TranslationUtil t;
	private final SimpMessagingTemplate messagingTemplate;

	@PostConstruct
	public void postConstruct(){
		log.info("WebSocketExceptionsHandler已创建");
	}

	@MessageExceptionHandler(BaseException.class)
	public void handleCustomException(BaseException e, Principal principal){
		log.debug("自定义异常：通用：", e);
		ExceptionEnum exceptionEnum = e.getExceptionEnum();
		messagingTemplate.convertAndSendToUser(
				principal.getName(),
				"/queue/chat/message",
				R.error(exceptionEnum.getErrorCode(), t.get("ex." + exceptionEnum.getErrorMsgKey(), e.getExceptionArgs())));
	}

	@MessageExceptionHandler(Exception.class)
	public void handleOtherException(Exception e, Principal principal){
		log.error("其他异常：", e);
		messagingTemplate.convertAndSendToUser(
				principal.getName(),
				"/queue/chat/message",
				R.error("ex.unknown"));
	}

}