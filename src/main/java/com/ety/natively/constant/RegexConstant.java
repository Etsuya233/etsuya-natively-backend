package com.ety.natively.constant;

/**
 * 正则表达式类
 */
public interface RegexConstant {
	String PHONE = "^1[3-9]\\d{9}$";
	String USERNAME = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{6,20}$";
	String PASSWORD = "^[a-zA-Z0-9_!#$%&*+,-.:;<=>?@^]{6,32}$";
	String NICKNAME = "^.{6,64}$";
	String EMAIL = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
	String GENDER = "^[0-2]$";
}
