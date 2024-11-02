package com.ety.natively.exception;

import com.ety.natively.enums.ExceptionEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

	private ExceptionEnum exceptionEnum;

	public BaseException() {
		super();
	}

	public BaseException(ExceptionEnum exceptionEnum) {
		super(exceptionEnum.getErrorMsgKey());
		this.exceptionEnum = exceptionEnum;
	}

}
