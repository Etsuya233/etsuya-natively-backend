package com.ety.natively.exception;

import com.ety.natively.enums.ExceptionEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

	private ExceptionEnum exceptionEnum;
	private Object[] exceptionArgs;

	public BaseException() {
		super();
	}

	public BaseException(ExceptionEnum exceptionEnum) {
		super(exceptionEnum.getErrorMsgKey());
		this.exceptionEnum = exceptionEnum;
	}

	public BaseException(ExceptionEnum exceptionEnum, Object... exceptionArgs) {
		super(exceptionEnum.getErrorMsgKey());
		this.exceptionEnum = exceptionEnum;
		this.exceptionArgs = exceptionArgs;
	}

}
