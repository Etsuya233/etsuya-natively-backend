package com.ety.natively.constant;

/**
 * 正则表达式类
 */
public interface RegexConstant {
	String PHONE = "^1[3-9]\\d{9}$";
	String USERNAME = "^(?!\\d+$)(?!_+$)[a-zA-Z0-9_]{6,18}$";
	String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d,.?#@$]{8,20}$";
	String NICKNAME = "^[a-zA-Z0-9_\\s\u4e00-\u9fa5]+$";
}
