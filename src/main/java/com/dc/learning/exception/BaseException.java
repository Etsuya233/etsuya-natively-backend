package com.dc.learning.exception;

import com.dc.learning.enums.ExceptionEnums;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Getter
@Setter
public class BaseException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	protected int code = 1000;
	/**
	 * 返回给用户的信息
	 */
	protected String msg = "error!";
	protected int status = 200;

	/**
	 * 默认构造函数
	 */
	public BaseException() {
		super();
	}

	/**
	 * 带有消息参数的构造函数
	 *
	 * @param message 异常的详细信息
	 */
	public BaseException(String message) {
		super(message);
	}

	/**
	 * 带有消息和用户返回信息的构造函数
	 *
	 * @param message 异常的详细信息
	 * @param msg 返回给用户的信息
	 */
	public BaseException(String message, String msg) {
		super(message);
		this.msg = msg;
	}

	/**
	 * 带有错误代码、消息和用户返回信息的构造函数
	 *
	 * @param code 错误代码
	 * @param message 异常的详细信息
	 * @param msg 返回给用户的信息
	 */
	public BaseException(int code, String message, String msg) {
		this(message, msg);
		this.code = code;
	}

	/**
	 * 带有消息和异常原因的构造函数
	 *
	 * @param message 异常的详细信息
	 * @param cause 导致此异常的原因
	 */
	public BaseException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 带有消息、用户返回信息和异常原因的构造函数
	 *
	 * @param message 异常的详细信息
	 * @param msg 返回给用户的信息
	 * @param cause 导致此异常的原因
	 */
	public BaseException(String message, String msg, Throwable cause) {
		super(message, cause);
		this.msg = msg;
	}

	/**
	 * 带有错误代码、消息、用户返回信息和异常原因的构造函数
	 *
	 * @param code 错误代码
	 * @param message 异常的详细信息
	 * @param msg 返回给用户的信息
	 * @param cause 导致此异常的原因
	 */
	public BaseException(int code, String message, String msg, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.msg = msg;
	}

	/**
	 * 带有错误代码、消息、用户返回信息、状态码和异常原因的构造函数
	 *
	 * @param code 错误代码
	 * @param message 异常的详细信息
	 * @param msg 返回给用户的信息
	 * @param status 自定义状态码
	 * @param cause 导致此异常的原因
	 */
	public BaseException(int code, String message, String msg, int status, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.msg = msg;
		this.status = status;
	}

	public BaseException(ExceptionEnums exceptionEnums){
		super(exceptionEnums.getMsg());
		this.code = exceptionEnums.getCode();
		this.msg = exceptionEnums.getMsg();
		this.status = exceptionEnums.getStatus();
	}

	public BaseException(ExceptionEnums exceptionEnums, Throwable cause){
		super(exceptionEnums.getMsg(), cause);
		this.code = exceptionEnums.getCode();
		this.msg = exceptionEnums.getMsg();
		this.status = exceptionEnums.getStatus();
	}

	public BaseException(ExceptionEnums exceptionEnums, String message){
		super(message);
		this.code = exceptionEnums.getCode();
		this.msg = exceptionEnums.getMsg();
		this.status = exceptionEnums.getStatus();
	}

	public BaseException(ExceptionEnums exceptionEnums, String message, Throwable cause){
		super(message, cause);
		this.code = exceptionEnums.getCode();
		this.msg = exceptionEnums.getMsg();
		this.status = exceptionEnums.getStatus();
	}
}
