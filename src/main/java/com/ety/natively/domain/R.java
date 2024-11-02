package com.ety.natively.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结果类
 * @param <T> 包裹的类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {
	private int code;
	private String msg;
	private T data;

	public static <T> R<T> ok(T data){
		return new R<>(200, null, data);
	}

	public static <T> R<T> error(String msg){
		return new R<>(1000, msg, null);
	}

	public static <T> R<T> error(int code, String msg){
		return new R<>(code, msg, null);
	}

	public static R<Void> ok(){
		return new R<>(200, null, null);
	}
}
