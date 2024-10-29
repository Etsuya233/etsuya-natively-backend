package com.dc.learning.enums;

import lombok.Getter;

@Getter
public enum ExceptionEnums {
	USER_NOT_FOUND(1001, "用户不存在", 200),
	PASSWORD_NOT_MATCH(1002, "密码错误", 200),
	USER_ACCESS_TOKEN_FAILED(1003, "用户登陆状态过期", 200),
	USER_STATUS_ERROR(1004, "用户状态异常", 200),
	USER_REFRESH_TOKEN_FAILED(1005, "用户登陆状态过期",200),

	CHECK_IN_FAILED(2000, "签到失败，可能是重复签到。", 200),

	AI_CONVERSATION_LIMIT(3000, "AI对话个数超出限制。", 200);

	private final int code;
	private final String msg;
	private final int status;

	ExceptionEnums(int code, String msg, int status) {
		this.code = code;
		this.msg = msg;
		this.status = status;
	}
}
